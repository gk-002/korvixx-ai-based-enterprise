package com.example.data

import com.example.data.database.RbacDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Service layer responsible for permission checks against the Room/SQLite database.
 */
class RbacPermissionService(private val rbacRepository: RbacRepository) {

    /**
     * Checks reactively if a user has a specific permission.
     */
    fun hasPermissionFlow(email: String, permissionId: String): Flow<Boolean> {
        return rbacRepository.getUserPermissions(email).map { permissions ->
            permissions.contains(permissionId)
        }
    }

    /**
     * Checks if a user has a specific permission (suspend function).
     */
    suspend fun hasPermission(email: String, permissionId: String): Boolean {
        return rbacRepository.hasPermission(email, permissionId)
    }

    /**
     * Retrieves all permissions assigned to a user's role.
     */
    fun getUserPermissions(email: String): Flow<List<String>> {
        return rbacRepository.getUserPermissions(email)
    }
}
