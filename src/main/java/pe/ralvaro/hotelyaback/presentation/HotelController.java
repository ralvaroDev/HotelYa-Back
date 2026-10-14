package pe.ralvaro.hotelyaback.presentation;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import pe.ralvaro.hotelyaback.application.CatalogService;
import static pe.ralvaro.hotelyaback.application.CatalogDtos.*;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {
    private final CatalogService catalog;
    public HotelController(CatalogService catalog) { this.catalog = catalog; }
    @GetMapping
    public List<HotelSummary> list() { return catalog.list(); }
    @GetMapping("/{id}")
    public HotelDetail detail(@PathVariable Long id) { return catalog.detail(id); }
}
