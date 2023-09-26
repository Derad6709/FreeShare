package com.a.freeshare.util

class FileUtil {

    companion object{

        private const val Byte = 1L
        private const val KB = 1000* Byte
        private const val MB = 1000* KB
        private const val GB = 1000* MB
        private const val TB = 1000* GB
        private const val PB = 1000* TB

        fun getFormattedLongData(bytes:Long):String{

            return when {
                bytes < KB -> {
                    String.format("%.2f Bytes",bytes.toFloat())
                }
                bytes >= PB -> {
                    String.format("%.2f PB",bytes / PB.toFloat())
                }
                bytes >= TB -> {
                    String.format("%.2f TB",bytes / TB.toFloat())
                }
                bytes >= GB -> {
                    String.format("%.2f GB",bytes / GB.toFloat())
                }
                bytes >= MB -> {
                    String.format("%.2f MB",bytes / MB.toFloat())
                }
                bytes >= KB -> {
                    String.format("%.2f KB",bytes / KB.toFloat())
                }
                else -> {
                    "???"
                }
            }
        }
    }
}