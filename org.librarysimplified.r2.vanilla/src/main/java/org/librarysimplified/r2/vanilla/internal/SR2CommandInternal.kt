package org.librarysimplified.r2.vanilla.internal

import org.joda.time.DateTime
import org.librarysimplified.r2.api.SR2Command
import java.util.UUID

/**
 * The type of internal commands. The set of internal commands is a superset of the commands
 * available via the API in order to implement retry logic and command logging.
 */

internal sealed class SR2CommandInternal {

  /**
   * The unique ID of the command.
   */

  abstract val id: UUID

  /**
   * The time the command was submitted.
   */

  abstract val submitted: DateTime

  /**
   * A command that produces a delay of [timeMilliseconds].
   */

  data class SR2CommandInternalDelay(
    override val id: UUID = UUID.randomUUID(),
    override val submitted: DateTime = DateTime.now(),
    val timeMilliseconds: Long
  ) : SR2CommandInternal()

  /**
   * A command that executes an API command.
   *
   * @see SR2Command
   */

  data class SR2CommandInternalAPI(
    override val id: UUID = UUID.randomUUID(),
    override val submitted: DateTime = DateTime.now(),
    val command: SR2Command
  ) : SR2CommandInternal()
}
