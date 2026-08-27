package com.example.thenobbery.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets ORDER BY timestamp DESC")
    fun getAllAssets(): Flow<List<Asset>>

    @Query("SELECT * FROM assets")
    suspend fun getAllAssetsSync(): List<Asset>

    @Query("SELECT * FROM assets WHERE uid = :id")
    suspend fun getAssetById(id: Long): Asset?

    @Query("SELECT * FROM assets WHERE title = :title LIMIT 1")
    suspend fun getAssetByTitle(title: String): Asset?

    @Query("SELECT * FROM assets WHERE pntrPaths != '[]' AND uid != :excludeId")
    fun getEligibleParents(excludeId: Long): Flow<List<Asset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: Asset): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @Transaction
    suspend fun insertAll(assets: List<Asset>): List<Long>

    @Update
    suspend fun updateAsset(asset: Asset)

    @Delete
    suspend fun deleteAsset(asset: Asset)

    // --- Lineage & Handshake Logic ---

    @Query("UPDATE assets SET sourceLinkId = :parentId WHERE uid = :childId")
    suspend fun linkToParent(childId: Long, parentId: Long)

    @Query("UPDATE assets SET sourceLinkId = 0 WHERE uid = :childId")
    suspend fun unlinkChild(childId: Long)

    @Query("SELECT * FROM assets WHERE sourceLinkId = :parentId AND uid != :parentId")
    suspend fun getChildrenForParent(parentId: Long): List<Asset>

    @Query("UPDATE assets SET sourceLinkId = :newParentId WHERE sourceLinkId = :oldParentId")
    suspend fun reLinkChildren(oldParentId: Long, newParentId: Long)

    @Query("UPDATE assets SET pntrPaths = :paths WHERE uid = :assetId")
    suspend fun transferPntrPaths(assetId: Long, paths: List<String>)
}
