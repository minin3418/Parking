package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Двор Парковка", appName)
  }

  @Test
  fun `verify GitHub decoupling and repository configuration`() {
    assertEquals("https://github.com/minin3418/Parking", com.example.data.network.ParkingApiClient.GITHUB_REPO_URL)
    assertEquals("https://raw.githubusercontent.com/minin3418/Parking/main/", com.example.data.network.ParkingApiClient.GITHUB_RAW_BASE_URL)
  }

  @Test
  fun `verify address normalization generates unique isolated IDs for different courtyards`() {
    val idKarbysheva22 = com.example.data.model.YardTemplates.normalizeAddressToId("ул. Карбышева, 22 / 22А")
    val idKarbysheva24 = com.example.data.model.YardTemplates.normalizeAddressToId("ул. Карбышева, 24")
    val idOkeansky = com.example.data.model.YardTemplates.normalizeAddressToId("Океанский проспект, 100")
    val idSvetlanskaya = com.example.data.model.YardTemplates.normalizeAddressToId("ул. Светланская, 45")

    assertEquals("karbysheva_22", idKarbysheva22)
    org.junit.Assert.assertNotEquals(idKarbysheva22, idKarbysheva24)
    org.junit.Assert.assertNotEquals(idKarbysheva22, idOkeansky)
    org.junit.Assert.assertNotEquals(idOkeansky, idSvetlanskaya)
  }

  @Test
  fun `verify yard data isolation when parking in different addresses`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.ParkingViewModel(app)

    // Select Yard 1: Karbysheva 22
    viewModel.selectYard("ул. Карбышева, 22 / 22А")
    viewModel.parkRow1(spotId = 1, carNumber = "А111АА", apt = "10", phone = "+79991112233")
    val yard1Spot1 = viewModel.uiState.value.data?.spots?.find { it.id == 1 }
    assertEquals("А111АА", yard1Spot1?.occupiedBy?.carNumber)

    // Select Yard 2: Okeansky 100
    viewModel.selectYard("Океанский проспект, 100")
    val yard2Spot1 = viewModel.uiState.value.data?.spots?.find { it.id == 1 }
    org.junit.Assert.assertNull("Spot 1 in Yard 2 must be free!", yard2Spot1?.occupiedBy)

    // Park in Yard 2
    viewModel.parkRow1(spotId = 1, carNumber = "В222ВВ", apt = "5", phone = "+79992223344")
    val yard2Spot1After = viewModel.uiState.value.data?.spots?.find { it.id == 1 }
    assertEquals("В222ВВ", yard2Spot1After?.occupiedBy?.carNumber)

    // Switch back to Yard 1: Spot 1 must STILL have car А111АА, NOT В222ВВ!
    viewModel.selectYard("ул. Карбышева, 22 / 22А")
    val yard1Spot1After = viewModel.uiState.value.data?.spots?.find { it.id == 1 }
    assertEquals("А111АА", yard1Spot1After?.occupiedBy?.carNumber)
  }
}
