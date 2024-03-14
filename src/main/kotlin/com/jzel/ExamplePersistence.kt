package com.jzel

import org.springframework.data.jpa.repository.JpaRepository

interface ExamplePersistence: JpaRepository<ExampleEntity, UInt>
