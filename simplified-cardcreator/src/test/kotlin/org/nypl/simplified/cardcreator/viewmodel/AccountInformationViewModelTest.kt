package org.nypl.simplified.cardcreator.viewmodel

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Rule
import org.junit.Test

import org.nypl.simplified.cardcreator.TestCoroutineRule
import org.nypl.simplified.cardcreator.network.CardCreatorService

class AccountInformationViewModelTest {
  private lateinit var subject: AccountInformationViewModel

  @get:Rule
  val testCoroutineRule = TestCoroutineRule()

  @MockK
  private lateinit var mockCardCreatorService: CardCreatorService

  @Before
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    subject = AccountInformationViewModel(mockCardCreatorService)
  }

  @Test
  fun `validatePassword causes validPassword state if between 8 and 32 alphanumeric characters`() {
    listOf(
      "goodPassword" to true,
      "badCharacters!@#$" to false,
      "shortpw" to false,
      "wayTooLongPasswordItGoesOnForever" to false
    ).forEach { (password, expectedValidity) ->
      subject.validatePassword(password)
      subject.state.value.validPassword shouldBe expectedValidity
    }
  }

  @Test
  fun `validateUsername causes validUsername state if between 5 and 25 characters`() {
    listOf(
      "goodUsername" to true,
      "nope" to false,
      "wayTooLongUsernameIsInvalid" to false,
    ).forEach { (password, expectedValidity) ->
      subject.validateUsername(password)
      subject.state.value.validUsername shouldBe expectedValidity
    }
  }
}
