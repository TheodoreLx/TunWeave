package io.github.theodorelx.tunweave

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.simpleproxy.tun2socks.Tun2socksJni
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeLibrarySmokeTest {

    @Test
    fun checkedInNativeLibraryLoadsAndRegistersJni() {
        assertTrue(
            "libhev-socks5-tunnel.so failed to load or register its JNI methods",
            Tun2socksJni.isLibraryLoaded,
        )
    }
}
