/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

interface Relay {
    suspend fun process(receiveFrom: ReceiveChannel<ByteArray>, sendTo: SendChannel<ByteArray>)
}