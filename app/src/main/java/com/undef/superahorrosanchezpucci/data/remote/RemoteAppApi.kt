package com.undef.superahorrosanchezpucci.data.remote

import com.undef.superahorrosanchezpucci.data.model.Categoria
import com.undef.superahorrosanchezpucci.data.model.ListaCompra
import com.undef.superahorrosanchezpucci.data.model.MetodoPago
import com.undef.superahorrosanchezpucci.data.model.Presupuesto
import com.undef.superahorrosanchezpucci.data.model.Producto
import com.undef.superahorrosanchezpucci.data.model.RolUsuario
import com.undef.superahorrosanchezpucci.data.model.Ticket
import com.undef.superahorrosanchezpucci.data.model.TicketProducto
import com.undef.superahorrosanchezpucci.data.model.TipoPresupuesto
import com.undef.superahorrosanchezpucci.data.model.Usuario
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.time.LocalDate
import kotlin.math.roundToInt

class RemoteAppApi {
    private var groupId: String? = null

    fun loadState(): RemoteAppState {
        val user = getMe()
        val group = ensureGroup()
        groupId = group.id

        val detail = getGroupDetail(group.id)
        val users = detail.members.ifEmpty {
            listOfNotNull(user?.toUsuario())
        }

        val budgets = getBudgets(group.id)
        val purchases = getPurchases(group.id)

        return RemoteAppState(
            presupuestos = budgets.ifEmpty { defaultBudgets() },
            listas = emptyList(),
            tickets = purchases,
            usuarios = users
        )
    }

    fun createBudget(name: String, amount: Int): Presupuesto {
        val gid = groupId ?: ensureGroup().id.also { groupId = it }
        val safeAmount = amount.toDouble().coerceAtLeast(1.0)
        val body = JSONObject()
            .put("name", name)
            .put("groupId", gid)
            .put("totalAmount", safeAmount)
            .put("items", JSONArray().put(JSONObject()
                .put("categoryId", firstCategoryId())
                .put("amount", safeAmount)
            ))
        return request("/api/budgets", "POST", body).dataObject().toBudget(active = true)
    }

    fun updateBudget(id: String, amount: Int): Presupuesto {
        val body = JSONObject()
            .put("totalAmount", amount.toDouble())
            .put("items", JSONArray().put(JSONObject()
                .put("categoryId", firstCategoryId())
                .put("amount", amount.toDouble().coerceAtLeast(1.0))
            ))
        return request("/api/budgets/$id", "PUT", body).dataObject().toBudget(active = true)
    }

    fun createProduct(producto: Producto): Producto {
        val categoryId = firstCategoryId()
        val body = productBody(producto, categoryId)
        return request("/api/products", "POST", body).dataObject()
            .toProducto(producto.cantidad, producto.comprado)
    }

    fun updateProduct(producto: Producto): Producto {
        val categoryId = firstCategoryId()
        val body = productBody(producto, categoryId)
        return request("/api/products/${producto.id}", "PUT", body).dataObject()
            .toProducto(producto.cantidad, producto.comprado)
    }

    fun deleteProduct(id: String) {
        request("/api/products/$id", "DELETE")
    }

    fun addPurchase(ticket: Ticket): Ticket {
        val gid = groupId ?: ensureGroup().id.also { groupId = it }
        val items = JSONArray()
        ticket.productos.forEach { item ->
            val productId = findProductId(item.nombre) ?: return@forEach
            items.put(JSONObject().put("productId", productId).put("quantity", item.cantidad))
        }
        if (items.length() == 0) {
            throw IllegalStateException("La compra necesita productos existentes en el backend.")
        }
        val body = JSONObject()
            .put("groupId", gid)
            .put("notes", ticket.supermercado)
            .put("items", items)
        return request("/api/purchases", "POST", body).dataObject().toTicket()
    }

    fun addMember(usuario: Usuario): Usuario {
        val gid = groupId ?: ensureGroup().id.also { groupId = it }
        val body = JSONObject()
            .put("email", usuario.email)
            .put("role", usuario.rol.toBackendRole())

        val data = request("/api/groups/$gid/members", "POST", body).dataObject()
        return data.toUsuario(default = usuario)
    }

    private fun getMe(): RemoteProfile? {
        return request("/api/users/me", "GET").dataObject().let {
            RemoteProfile(
                id = it.optString("id"),
                fullName = it.optString("fullName").ifBlank { it.optString("name") },
                email = it.optString("email"),
                role = it.optString("role")
            )
        }
    }

    private fun ensureGroup(): RemoteGroup {
        val groups = request("/api/groups", "GET").dataArray().mapObjects { it.toGroup() }
        groups.firstOrNull()?.let { return it }
        val created = request(
            "/api/groups",
            "POST",
            JSONObject().put("name", "Familia").put("description", "Grupo familiar de Super Ahorro")
        ).dataObject()
        return created.toGroup()
    }

    private fun getGroupDetail(id: String): RemoteGroupDetail {
        val data = request("/api/groups/$id", "GET").dataObject()
        return RemoteGroupDetail(
            members = data.optJSONArray("members").orEmpty().mapObjects { member ->
                member.toUsuario()
            }
        )
    }

    private fun getBudgets(groupId: String): List<Presupuesto> {
        val encoded = URLEncoder.encode(groupId, "UTF-8")
        return request("/api/budgets?groupId=$encoded", "GET")
            .dataArray()
            .mapObjectsIndexed { index, item -> item.toBudget(active = index == 0) }
    }

    private fun getProducts(): List<Producto> {
        return request("/api/products", "GET")
            .dataArray()
            .mapObjects { it.toProducto() }
    }

    private fun getPurchases(groupId: String): List<Ticket> {
        val encoded = URLEncoder.encode(groupId, "UTF-8")
        return request("/api/purchases?groupId=$encoded", "GET")
            .dataArray()
            .mapObjects { it.toTicket() }
    }

    private fun firstCategoryId(): String {
        return request("/api/categories", "GET")
            .dataArray()
            .mapObjects { it.optString("id") }
            .firstOrNull { it.isNotBlank() }
            ?: throw IllegalStateException("No hay categorías disponibles en el backend.")
    }

    private fun findProductId(name: String): String? {
        return getProducts().firstOrNull { it.nombre.equals(name, ignoreCase = true) }?.id
    }

    private fun productBody(producto: Producto, categoryId: String): JSONObject {
        return JSONObject()
            .put("name", producto.nombre)
            .put("price", (producto.precioEstimado.takeIf { it > 0 } ?: producto.precio).toDouble())
            .put("categoryId", categoryId)
            .put("description", producto.descripcion)
            .put("barcode", producto.codigo)
            .put("priority", producto.categoria.toBackendPriority())
    }

    private fun request(path: String, method: String, body: JSONObject? = null): JSONObject {
        val token = AuthSessionStore.accessToken
            ?: throw IllegalStateException("Iniciá sesión para sincronizar con Render.")
        val connection = (URL("${ApiConfig.BASE_URL}$path").openConnection() as HttpURLConnection)
        connection.requestMethod = method
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000

        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream ?: connection.inputStream
        val text = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
        connection.disconnect()

        val trimmed = text.trim()
        val json = when {
            trimmed.isBlank() -> JSONObject()
            trimmed.startsWith("[") -> JSONObject().put("data", JSONArray(trimmed))
            else -> JSONObject(trimmed)
        }
        if (responseCode !in 200..299 || json.optBoolean("success", true).not()) {
            val message = json.optString("message")
                .ifBlank { json.optString("error") }
                .ifBlank { "Error del servidor ($responseCode)" }
            throw IllegalStateException(message)
        }
        return json
    }

    private fun JSONObject.dataObject(): JSONObject {
        return optJSONObject("data") ?: this
    }

    private fun JSONObject.dataArray(): JSONArray {
        return optJSONArray("data")
            ?: optJSONArray("items")
            ?: optJSONArray("results")
            ?: JSONArray()
    }

    private fun JSONArray?.orEmpty(): JSONArray = this ?: JSONArray()

    private fun <T> JSONArray.mapObjects(mapper: (JSONObject) -> T): List<T> {
        return (0 until length()).mapNotNull { index -> optJSONObject(index)?.let(mapper) }
    }

    private fun <T> JSONArray.mapObjectsIndexed(mapper: (Int, JSONObject) -> T): List<T> {
        return (0 until length()).mapNotNull { index -> optJSONObject(index)?.let { mapper(index, it) } }
    }

    private fun JSONObject.toGroup(): RemoteGroup {
        return RemoteGroup(id = optId(), name = optString("name"))
    }

    private fun JSONObject.toBudget(active: Boolean): Presupuesto {
        val total = optDouble("totalAmount", 0.0).roundToInt()
        return Presupuesto(
            id = optId(),
            tipo = TipoPresupuesto.FAMILIAR,
            nombre = optString("name").ifBlank { "Familiar" },
            montoTotal = total,
            montoDisponible = total,
            fechaInicio = System.currentTimeMillis(),
            fechaFin = null,
            activo = active
        )
    }

    private fun JSONObject.toProducto(cantidad: Int = 1, comprado: Boolean = false): Producto {
        val price = optDouble("price", 0.0).roundToInt()
        return Producto(
            id = optId(),
            codigo = optString("barcode"),
            nombre = optString("name"),
            descripcion = optString("description"),
            precio = price,
            precioEstimado = price,
            cantidad = cantidad,
            comprado = comprado,
            categoria = optString("priority").toCategoria()
        )
    }

    private fun JSONObject.toTicket(): Ticket {
        val items = optJSONArray("items").orEmpty().mapObjects {
            val product = it.optJSONObject("product")
            TicketProducto(
                nombre = it.optString("productName")
                    .ifBlank { product?.optString("name").orEmpty() },
                precio = optDoubleCompat(it, "unitPrice").roundToInt(),
                cantidad = it.optInt("quantity", 1)
            )
        }
        return Ticket(
            id = optId(),
            supermercado = optString("storeName").ifBlank { optString("notes").ifBlank { "Supermercado" } },
            direccion = "",
            fechaHora = System.currentTimeMillis(),
            total = optDoubleCompat(this, "total").roundToInt(),
            metodoPago = MetodoPago.EFECTIVO,
            imagenPath = "",
            presupuestoId = optString("groupId"),
            productos = items
        )
    }

    private fun JSONObject.toUsuario(default: Usuario? = null): Usuario {
        val user = optJSONObject("user") ?: optJSONObject("usuario") ?: this
        val role = optString("role")
            .ifBlank { user.optString("role") }
            .ifBlank { default?.rol?.name.orEmpty() }

        return Usuario(
            id = user.optId().ifBlank { default?.id.orEmpty() },
            nombre = user.optString("fullName")
                .ifBlank { user.optString("name") }
                .ifBlank { default?.nombre.orEmpty() },
            email = user.optString("email").ifBlank { default?.email.orEmpty() },
            rol = if (role.equals("ADMIN", ignoreCase = true)) RolUsuario.ADMIN else RolUsuario.MIEMBRO
        )
    }

    private fun RemoteProfile.toUsuario(): Usuario {
        return Usuario(
            id = id,
            nombre = fullName,
            email = email,
            rol = if (role == "ADMIN") RolUsuario.ADMIN else RolUsuario.MIEMBRO
        )
    }

    private fun Categoria.toBackendPriority(): String {
        return when (this) {
            Categoria.ESENCIAL -> "ESENCIAL"
            Categoria.PRINCIPAL -> "PRIMARIO"
            Categoria.SECUNDARIO -> "SECUNDARIO"
        }
    }

    private fun RolUsuario.toBackendRole(): String {
        return if (this == RolUsuario.ADMIN) "ADMIN" else "MEMBER"
    }

    private fun JSONObject.optId(): String {
        return optString("id").ifBlank { optString("_id") }
    }

    private fun String.toCategoria(): Categoria {
        return when (uppercase()) {
            "ESENCIAL" -> Categoria.ESENCIAL
            "PRIMARIO", "PRINCIPAL" -> Categoria.PRINCIPAL
            else -> Categoria.SECUNDARIO
        }
    }

    private fun optDoubleCompat(json: JSONObject, key: String): Double {
        val raw = json.opt(key)
        return when (raw) {
            is Number -> raw.toDouble()
            is String -> raw.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    private fun defaultBudgets(): List<Presupuesto> {
        return listOf(
            Presupuesto("presupuesto-familiar", TipoPresupuesto.FAMILIAR, "Familiar", 0, 0, System.currentTimeMillis(), null, true),
            Presupuesto("presupuesto-individual", TipoPresupuesto.INDIVIDUAL, "Individual", 0, 0, System.currentTimeMillis(), null, false)
        )
    }

    private data class RemoteProfile(val id: String, val fullName: String, val email: String, val role: String)
    private data class RemoteGroup(val id: String, val name: String)
    private data class RemoteGroupDetail(val members: List<Usuario>)
}

data class RemoteAppState(
    val presupuestos: List<Presupuesto>,
    val listas: List<ListaCompra>,
    val tickets: List<Ticket>,
    val usuarios: List<Usuario>
)
