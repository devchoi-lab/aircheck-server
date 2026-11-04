package com.seriouschoi.aircheck.core.persistence

import jakarta.persistence.*
import java.time.LocalDateTime


@Entity
@Table(
    name = "api_call_log",
    indexes = [
        Index(name = "idx_api_call_log_called_at", columnList = "calledAt"),
        Index(name = "idx_api_call_log_type_called_at", columnList = "apiType, calledAt")
    ]
)
class ApiCallLogEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false, length = 50)
    val apiType: String,
    
    @Column(nullable = false)
    val calledAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(nullable = false)
    val success: Boolean,
    
    @Column(nullable = false)
    val responseTimeMs: Long = 0,
    
    @Column(length = 500)
    val errorMessage: String? = null
)
