package org.librarysimplified.r2.api

/**
 * A command queue that accepts commands from any thread and executes them serially
 * on a single thread.
 */

interface SR2ControllerCommandQueueType {

  /**
   * Execute a command asynchronously.
   */

  fun submitCommand(command: SR2Command)
}
