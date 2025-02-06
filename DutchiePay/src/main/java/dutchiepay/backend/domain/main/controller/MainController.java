package dutchiepay.backend.domain.main.controller;

import dutchiepay.backend.domain.main.dto.MainResponseDto;
import dutchiepay.backend.domain.main.service.MainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/main")
@RequiredArgsConstructor
public class MainController {

    private final MainService mainService;
    @GetMapping
    @PreAuthorize("permitAll()")
    public MainResponseDto getMain() {

        return mainService.getMain();
    }

    @GetMapping("/server-info")
    public ResponseEntity<?> getServerInfo() {
        return ResponseEntity.ok().body(mainService.getHostName());
    }
}
