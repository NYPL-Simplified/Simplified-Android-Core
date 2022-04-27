package org.nypl.simplified.cardcreator.viewmodel

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
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
  fun `validate password emits success`() {
    subject.validatePassword("goodPassword")
  }

  @Test
  fun `validate password emits failure`() {
    subject.validatePassword("badPassword!@#$%")
  }
}
