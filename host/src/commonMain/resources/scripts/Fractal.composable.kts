import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Data
import org.jetbrains.skia.RuntimeEffect
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class Shader(val shader: String) {
    MANDELBROT(
        shader = """
            uniform vec2 iResolution; // Use vec3 for resolution (width, height, pixelRatio)
            uniform float x_min, y_min, x_max, y_max;
        
            const float N = 1024.0;
            const float B = 256.0;
            const float SS = 2.0;
        
            float random(in vec2 st) {
                return fract(sin(dot(st.xy, vec2(12.9898,78.233))) * 43758.5453123);
            }
        
            vec2 random2(in vec2 st) {
                return vec2(random(st), random(st + vec2(1.0, 1.0)));
            }
        
            vec3 pal(in float t, in vec3 a, in vec3 b, in vec3 c, in vec3 d) {
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
                    float x = mix(x_min, x_max, (fragCoord.x + random2(R+i).x) / R.x);
                    float y = mix(y_min, y_max, (fragCoord.y + random2(R+i).y) / R.y);
                    vec2 uv = vec2(x, y);
                    float sn = iterate(uv) / N;
                    col += pal(fract(2.0*sn + 0.5), vec3(0.5), vec3(0.5),
                               vec3(1.0,1.0,1.0), vec3(0.0, 0.10, 0.2));
                }
        
                return half4(col / SS, 1.0);
            }
        """.trimIndent()
    ) {
        override fun buildBuffer(
            size: Size,
            minX: Float,
            maxX: Float,
            minY: Float,
            maxY: Float
        ): ByteBuffer {
            return ByteBuffer
                .allocate(24)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putFloat(size.width)
                .putFloat(size.height)
                .putFloat(minX)
                .putFloat(minY)
                .putFloat(maxX)
                .putFloat(maxY)
                .rewind()
        }
    },
    BURNING_SHIP(
        shader = """
            uniform vec2 iResolution;
            uniform float x_min, y_min, x_max, y_max;
            const int max_iter = 512;
            
            // (a + bi)(c + di) = (ac−bd) + (ad+bc)i
            vec2 cplx_mul(vec2 a, vec2 b) {
                return vec2(a.x * b.x - a.y * b.y, a.x * b.y + a.y * b.x);
            }
            
            // |a + bi| = sqrt(a^2 + b^2)
            float cplx_abs(vec2 a) {
                return sqrt(a.x * a.x + a.y * a.y);
            }
            
            vec4 main(vec2 fragCoord) {
                float x = mix(x_min, x_max, fragCoord.x / iResolution.x);
                float y = mix(y_min, y_max, fragCoord.y / iResolution.y);
                vec2 c = vec2(x, y);
                
                vec2 z = vec2(0.0, 0.0);
                vec3 col = vec3(0.0, 0.0, 0.0);

                for(int i = 0; i < max_iter; i++) {
                    z = vec2(abs(z.x), abs(z.y));
                    z = cplx_mul(z, z) + c;
                    
                    if (cplx_abs(z) > 2.0) {
                        float t = float(i) / 256;
                        t = sqrt(t);
                        t = smoothstep(0.0, 1.0, t);
                        t = (1 - t) * 2;
                        float k = floor(t);
                        k = smoothstep(0.0, 1.0, k);
                        float f = fract(k);
                        vec3 col1 = vec3(t, t * t, t * 3);
                        vec3 col2 = vec3(k, k * k, k * 3);
                        col = mix(col1, col2, t);
                    }
                }
                
                return vec4(col, 1.0);
            }
        """.trimIndent()
    ) {
        var minX = -2.5f
        var minY = -2.5f
        var maxX = 1.5f
        var maxY = 1.5f

        override fun buildBuffer(
            size: Size,
            minX: Float,
            maxX: Float,
            minY: Float,
            maxY: Float
        ): ByteBuffer {
            return ByteBuffer
                .allocate(24)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putFloat(size.width)
                .putFloat(size.height)
                .putFloat(minX)
                .putFloat(minY)
                .putFloat(maxX)
                .putFloat(maxY)
                .rewind()
        }
    };

    abstract fun buildBuffer(
        size: Size,
        minX: Float,
        maxX: Float,
        minY: Float,
        maxY: Float
    ): ByteBuffer
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun Content() {
    var activeShader by remember { mutableStateOf(Shader.BURNING_SHIP) }
    val shaderEffect = remember(activeShader) {
        RuntimeEffect.makeForShader(activeShader.shader)
    }
    var minX by remember { mutableFloatStateOf(-2.5f) }
    var minY by remember { mutableFloatStateOf(-2.5f) }
    var maxX by remember { mutableFloatStateOf(1.5f) }
    var maxY by remember { mutableFloatStateOf(1.5f) }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val dx = dragAmount.x * (maxX - minX) / size.width
                        val dy = dragAmount.y * (maxY - minY) / size.height

                        minX -= dx
                        maxX -= dx
                        minY -= dy
                        maxY -= dy
                    }
                }
                .onPointerEvent(PointerEventType.Scroll) {
                    it.changes.forEach { change ->
                        val scrollDelta = change.scrollDelta.y
                        val zoomFactor = 1f - scrollDelta * 0.05f

                        // Calculate mouse position as fraction of view (0.0 to 1.0)
                        val viewX = change.position.x / size.width
                        val viewY = change.position.y / size.height

                        // Convert to actual fractal coordinates
                        val fractalX = minX + viewX * (maxX - minX)
                        val fractalY = minY + viewY * (maxY - minY)

                        // Calculate new min/max while keeping the point under cursor fixed
                        minX = fractalX - (fractalX - minX) / zoomFactor
                        maxX = fractalX + (maxX - fractalX) / zoomFactor
                        minY = fractalY - (fractalY - minY) / zoomFactor
                        maxY = fractalY + (maxY - fractalY) / zoomFactor

                        // Keep a sensible zoom limit
                        val currentRange = maxX - minX
                        if (currentRange < 0.000001f || currentRange > 10f) {
                            // Revert changes if we hit zoom limits
                            minX = minX * zoomFactor
                            maxX = maxX * zoomFactor
                            minY = minY * zoomFactor
                            maxY = maxY * zoomFactor
                        }

                        change.consume()
                    }
                }
                .drawBehind {
                    val resolutionBuffer = activeShader.buildBuffer(size, minX, maxX, minY, maxY)

                    val brush = ShaderBrush(
                        shaderEffect.makeShader(
                            uniforms = Data.makeFromBytes(resolutionBuffer.array()),
                            children = null,
                            localMatrix = null
                        )
                    )
                    drawRect(brush)
                }
        ) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Min ($minX, $minY) Max ($maxX, $maxY)"
                    )
                }
            }
        }

        Surface {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Center
            ) {
                Shader.entries.forEach { shader ->
                    TextButton(
                        onClick = { activeShader = shader }
                    ) {
                        Text(shader.name)
                    }
                }
            }
        }
    }
}