package pe.ralvaro.hotelyaback.owner;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.util.Objects;
class MediaContentTests {
    private byte[] fixture(String name) throws Exception {
        try(var input=Objects.requireNonNull(getClass().getResourceAsStream("/media/"+name))) { return input.readAllBytes(); }
    }
    @Test void acceptsRealPngAndJpegRatherThanDeclaredExtensions() throws Exception {
        for(String format:new String[]{"png","jpeg"}) {
            var out=new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(3,3,BufferedImage.TYPE_INT_RGB),format,out);
            assertEquals("image/"+format,MediaService.detect(out.toByteArray()));
        }
    }
    @Test void rejectsTruncatedImagesAndFakeWebpMp4() {
        assertNull(MediaService.detect("<script>alert(1)</script>".getBytes()));
        assertNull(MediaService.detect(new byte[]{(byte)255,(byte)216,(byte)255,0,0}));
        assertNull(MediaService.detect("RIFFaaaaaaaaWEBPVP8 aaa".getBytes()));
        assertNull(MediaService.detect(new byte[]{0,0,0,16,'f','t','y','p','m','p','4','2',0,0,0,0}));
    }
    @Test void decodesActualWebpContent() throws Exception {
        assertEquals("image/webp",MediaService.detect(fixture("valid.webp")));
    }
    @Test void acceptsExistingMp4WithActualVideoTrack() throws Exception {
        assertEquals("video/mp4",MediaService.detect(fixture("valid.mp4")));
    }
}
