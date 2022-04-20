package org.nypl.simplified.cardcreator.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.shouldBe
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
  fun `confirmCard emits confirmCard event`() = runBlockingTest {
    var emitted: ConfirmationEvent? = null
    launch {
      emitted = subject.events.first()
    }

    subject.confirmCard()

    emitted shouldBe ConfirmationEvent.CardConfirmed
  }

  @Test
  fun `prepareCardForSave emits permissions check event`() = runBlockingTest {
    var emitted: ConfirmationEvent? = null
    launch {
      emitted = subject.events.first()
    }

    subject.confirmCard()

    emitted shouldBe ConfirmationEvent.SaveCardPermissionsCheck
  }

  @Ignore
  @Test
  fun createAndStoreDigitalCard() {
    TODO("Not yet implemented")
  }
}
