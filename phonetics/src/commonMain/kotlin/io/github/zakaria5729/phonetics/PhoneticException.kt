package io.github.zakaria5729.phonetics

/**
 * Base type of every problem the library reports.
 *
 * Errors are returned in result objects, not thrown.
 */
public sealed class PhoneticException(public override val message: String) : Exception(message)

/** The request was cancelled before it finished. */
public class PhoneticCancelledException(
    message: String = "The phonetics request was cancelled",
) : PhoneticException(message) {
    override fun toString(): String = "PhoneticCancelledException: $message"
}

/** An argument, rule or language definition was not valid. */
public class PhoneticArgumentException(message: String) : PhoneticException(message) {
    override fun toString(): String = "PhoneticArgumentException: $message"
}

/** The instance was disposed and cannot run requests. */
public class PhoneticDisposedException(
    message: String = "Phonetics was disposed",
) : PhoneticException(message) {
    override fun toString(): String = "PhoneticDisposedException: $message"
}

/** The background worker failed. */
public class PhoneticWorkerException(message: String) : PhoneticException(message) {
    override fun toString(): String = "PhoneticWorkerException: $message"
}
