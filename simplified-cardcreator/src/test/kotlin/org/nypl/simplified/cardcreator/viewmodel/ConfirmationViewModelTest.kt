package org.nypl.simplified.cardcreator.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.nypl.simplified.cardcreator.TestCoroutineRule

@ExperimentalCoroutinesApi
class ConfirmationViewModelTest {
  private lateinit var subject: ConfirmationViewModel

  @get:Rule
  val testCoroutineRule = TestCoroutineRule()

  @Before
  fun setUp() {
    val testData = ConfirmationData(
      "testName",
      "testBarcode",
      "testPassword",
      "testMessage"
    )
    subject = ConfirmationViewModel(testData)
  }

  @Test
  fun `confirmCard emits confirmCard event`() {
    subject.state.value.events.isEmpty() shouldBe true
    subject.confirmCard()
    val events = subject.state.value.events
    events.first() shouldBe ConfirmationEvent.CardConfirmed
    events.size shouldBeEqualTo 1
  }

  @Test
  fun `prepareCardForSave emits permissions check event`() {
    subject.state.value.events.isEmpty() shouldBe true
    subject.prepareToSaveCard()
    val events = subject.state.value.events
    events.first() shouldBe ConfirmationEvent.SaveCardPermissionsCheck
    events.size shouldBeEqualTo 1
  }

  @Ignore
  @Test
  fun createAndStoreDigitalCard() {
    TODO("Not yet implemented")
  }
}
