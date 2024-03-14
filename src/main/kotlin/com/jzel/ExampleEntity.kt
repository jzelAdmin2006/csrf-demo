package com.jzel

import jakarta.persistence.*

@Entity
@Table(name = "data")
data class ExampleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,
    val name: String,
)
