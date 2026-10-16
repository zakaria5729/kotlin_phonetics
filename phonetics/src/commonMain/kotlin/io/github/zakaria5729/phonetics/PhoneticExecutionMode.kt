package io.github.zakaria5729.phonetics

/** What to do when a request arrives while another one is running. */
public enum class PhoneticExecutionMode {
    /** Run requests one after another. */
    Queue,

    /** Cancel the running request and start the new one. This is the default. */
    CancelPrevious,
}
