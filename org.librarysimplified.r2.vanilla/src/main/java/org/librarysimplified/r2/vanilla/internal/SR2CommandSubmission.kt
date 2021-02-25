package org.librarysimplified.r2.vanilla.internal

import org.joda.time.DateTime
import org.librarysimplified.r2.api.SR2Command
import java.util.UUID

/**
 * The type of internal commands. The set of internal commands is a superset of the commands
 * available via the API in order to implement retry logic and command logging.
 */

data class SR2CommandSubmission(

  /**
   * The unique ID of the command.
   */

  val id: UUID = UUID.randomUUID(),

  /**
   * The time the command was submitted.
   */

  val submitted: DateTime = DateTime.now(),

  /**
   * A command that executes an API command.
   *
   * @see SR2Command
   */

  val command: SR2Command
)
