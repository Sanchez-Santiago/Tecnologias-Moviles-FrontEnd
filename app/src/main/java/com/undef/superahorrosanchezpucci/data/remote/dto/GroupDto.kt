package com.undef.superahorrosanchezpucci.data.remote.dto

data class GroupResponse(
    val id: String,
    val name: String,
    val description: String? = null,
    val categoria: String? = "FAMILIA",
    val createdBy: String,
    val memberCount: Int,
    val role: String,
    val createdAt: String = ""
)

data class GroupDetailResponse(
    val id: String,
    val name: String,
    val description: String? = null,
    val categoria: String? = "FAMILIA",
    val createdBy: String,
    val members: List<GroupMemberResponse>,
    val createdAt: String = ""
)

data class GroupMemberResponse(
    val id: String,
    val fullName: String,
    val email: String,
    val role: String,
    val joinedAt: String = ""
)

data class InvitationResponse(
    val id: String,
    val groupId: String,
    val groupName: String,
    val invitedBy: String,
    val invitedByEmail: String,
    val status: String,
    val token: String,
    val expiresAt: String = "",
    val createdAt: String = ""
)

data class InviteRequest(
    val email: String
)

data class CreateGroupRequest(
    val name: String,
    val description: String? = null,
    val categoria: String? = null
)
