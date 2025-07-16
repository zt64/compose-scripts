import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import org.intellij.lang.annotations.Language
import org.jetbrains.skia.Data
import org.jetbrains.skia.RuntimeEffect
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Language("sksl")
val shader = """
    uniform vec2 iResolution; // Use vec3 for resolution (width, height, pixelRatio)
    uniform float iZoom; // Zoom level
    uniform vec2 iOffset; // X and Y offset

    const float N = 256.0;
    const float B = 32.0;
    const float SS = 4.0;

    float random (in vec2 st) {
        return fract(sin(dot(st.xy, vec2(12.9898,78.233))) * 43758.5453123);
    }

    vec2 random2(in vec2 st) {
        return vec2(random(st), random(st + vec2(1.0, 1.0)));
    }

    vec3 pal( in float t, in vec3 a, in vec3 b, in vec3 c, in vec3 d ) {
        return a + b*cos(6.28318 * (c*t + d));
    }

    float iterate(vec2 p) {
        vec2 z = vec2(0.0), c = p;
        float iVal = 0.0;

        for (float i = 0.0; i < N; i++) {
            z = vec2(z.x*z.x - z.y*z.y, 2.0*z.x*z.y) + c;
            if (dot(z, z) > (B*B)) {
                iVal = i;
                break;
            }
            iVal = i + 1.0;
        }

        return iVal - log(log(dot(z, z)) / log(B)) / log(2.0);
    }

    half4 main(vec2 fragCoord) {
        vec2 R = iResolution.xy;
        vec3 col = vec3(0.0);

        for(float i=0.0; i < SS; i++) {
            vec2 uv = (((2.0 * fragCoord + random2(R+i) - R) / R.y) + iOffset) / iZoom;


            float sn = iterate(uv) / N;
            col += pal(fract(2.0*sn + 0.5), vec3(0.5), vec3(0.5),
                       vec3(1.0,1.0,1.0), vec3(0.0, 0.10, 0.2));
        }

        return half4(col / SS, 1.0);
    }
""".trimIndent()

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Content() {
    val shaderEffect = remember { RuntimeEffect.makeForShader(shader) }
    var zoom by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPointerEvent(PointerEventType.Scroll) {
                it.changes.forEach { change ->
                    val scrollDelta = change.scrollDelta.y
                    // Invert scroll delta for natural zoom direction
                    val zoomFactor = (1f - scrollDelta / 20f)
                    val newZoom = (zoom * zoomFactor).coerceAtLeast(1f)

                    // Get cursor position relative to the center of the composable
                    val cursorX = change.position.x - size.width / 2f
                    val cursorY = change.position.y - size.height / 2f

                    // Convert cursor position to shader's UV coordinate space
                    val uvX = (2f * cursorX / size.height + offsetX) / zoom
                    val uvY = (2f * cursorY / size.height + offsetY) / zoom

                    // Calculate the new offset to keep the cursor point stationary
                    offsetX = uvX * newZoom - 2f * cursorX / size.height
                    offsetY = uvY * newZoom - 2f * cursorY / size.height

                    zoom = newZoom

                    change.consume()
                }
            }
            .drawBehind {
                val resolutionBuffer = ByteBuffer
                    .allocate(20)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putFloat(size.width)
                    .putFloat(size.height)
                    .putFloat(zoom)
                    .putFloat(offsetX)
                    .putFloat(offsetY)
                    .rewind()

                val brush = ShaderBrush(
                    shaderEffect.makeShader(
                        uniforms = Data.makeFromBytes(resolutionBuffer.array()),
                        children = null,
                        localMatrix = null
                    )
                )
                drawRect(brush)
            }
    )
}