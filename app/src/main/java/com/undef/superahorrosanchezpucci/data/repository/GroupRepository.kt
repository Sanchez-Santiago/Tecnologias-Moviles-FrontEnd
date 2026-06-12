package com.undef.superahorrosanchezpucci.data.repository

import android.util.Log
import com.undef.superahorrosanchezpucci.data.local.AppDao
import com.undef.superahorrosanchezpucci.data.local.GrupoEntity
import com.undef.superahorrosanchezpucci.data.local.InvitacionEntity
import com.undef.superahorrosanchezpucci.data.remote.ApiService
import com.undef.superahorrosanchezpucci.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class GroupRepository(
    private val apiService: ApiService,
    private val appDao: AppDao
) {
    suspend fun getGroups(): List<GroupDetailResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMyGroups()
            if (response.isSuccessful && response.body()?.success == true) {
                val groupsList = response.body()?.data ?: emptyList()
                val details = groupsList.mapNotNull { g ->
                    val detail = apiService.getGroupById(g.id)
                    if (detail.isSuccessful) detail.body()?.data else null
                }
                
                if (details.isNotEmpty()) {
                    appDao.clearGrupos()
                    appDao.insertGrupos(details.map { it.toEntity() })
                }
                return@withContext details
            }
        } catch (e: Exception) {
            Log.e("GroupRepo", "Error fetching groups", e)
        }
        appDao.getGrupos().map { it.toModel() }
    }

    suspend fun createGroup(name: String, category: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createGroup(CreateGroupRequest(name, categoria = category))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al crear grupo"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInvitations(): List<InvitationResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMyInvitations()
            if (response.isSuccessful && response.body()?.success == true) {
                val invitations = response.body()?.data ?: emptyList()
                appDao.clearInvitaciones()
                appDao.insertInvitaciones(invitations.map { it.toEntity() })
                return@withContext invitations
            }
        } catch (_: Exception) {}
        appDao.getInvitaciones().map { it.toModel() }
    }

    suspend fun inviteMember(groupId: String, email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.inviteMember(groupId, InviteRequest(email))
            if (response.isSuccessful && response.body()?.success == true) Result.success(Unit)
            else Result.failure(Exception(response.body()?.message ?: "Error al invitar"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun acceptInvitation(token: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.acceptInvitation(token)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.body()?.message ?: "Error al aceptar"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun rejectInvitation(token: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.rejectInvitation(token)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.body()?.message ?: "Error al rechazar"))
        } catch (e: Exception) { Result.failure(e) }
    }

    // Helpers Mappings
    private fun GroupDetailResponse.toEntity() = GrupoEntity(
        id = id, name = name, description = description, categoria = categoria ?: "FAMILIA",
        createdBy = createdBy, membersJson = members.toMembersJson(), createdAt = createdAt
    )

    private fun GrupoEntity.toModel() = GroupDetailResponse(
        id = id, name = name, description = description, categoria = categoria ?: "FAMILIA",
        createdBy = createdBy, members = membersJson.parseMembers(), createdAt = createdAt
    )

    private fun InvitationResponse.toEntity() = InvitacionEntity(
        id, groupId, groupName, invitedBy, invitedByEmail, status, token, expiresAt, createdAt
    )

    private fun InvitacionEntity.toModel() = InvitationResponse(
        id, groupId, groupName, invitedBy, invitedByEmail, status, token, expiresAt, createdAt
    )

    private fun List<GroupMemberResponse>.toMembersJson(): String = JSONArray(
        map { m -> JSONObject().apply { put("id", m.id); put("fullName", m.fullName); put("email", m.email); put("role", m.role); put("joinedAt", m.joinedAt) } }
    ).toString()

    private fun String.parseMembers(): List<GroupMemberResponse> = try {
        val arr = JSONArray(this)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            GroupMemberResponse(o.getString("id"), o.getString("fullName"), o.getString("email"), o.getString("role"), o.optString("joinedAt", ""))
        }
    } catch (_: Exception) { emptyList() }
}
