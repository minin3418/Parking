package com.example

import com.example.data.model.ParkingLandmark
import com.example.data.model.ParkingSpot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testSpotCreationAndAngleNormalization() {
    val spot = ParkingSpot(
      id = 10,
      x = 100f,
      y = 120f,
      angle = 45f
    )
    assertEquals(10, spot.id)
    assertEquals(45f, spot.angle ?: 0f, 0.001f)
    assertTrue(spot.isFree)
  }

  @Test
  fun testLandmarkCreation() {
    val lm = ParkingLandmark(
      id = "lm_barrier",
      text = "🚧 Шлагбаум",
      x = 50f,
      y = 80f
    )
    assertEquals("🚧 Шлагбаум", lm.text)
    assertEquals(50f, lm.x, 0.001f)
    assertEquals(80f, lm.y, 0.001f)
  }

  @Test
  fun testRowCreationCalculation() {
    val startId = 1
    val count = 4
    val stepX = 42f
    val startX = 100f

    val generatedSpots = (0 until count).map { i ->
      ParkingSpot(
        id = startId + i,
        x = startX + i * stepX,
        y = 150f,
        angle = 0f
      )
    }

    assertEquals(4, generatedSpots.size)
    assertEquals(1, generatedSpots[0].id)
    assertEquals(100f, generatedSpots[0].x, 0.001f)
    assertEquals(4, generatedSpots[3].id)
    assertEquals(226f, generatedSpots[3].x, 0.001f)
  }
}
