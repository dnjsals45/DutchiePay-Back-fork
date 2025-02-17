package dutchiepay.backend.entity;

import dutchiepay.backend.domain.community.dto.UpdateFreeRequestDto;
import dutchiepay.backend.global.config.Auditing;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Table(name = "Free")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Free extends Auditing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long freeId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 60, nullable = false)
    private String title;

    @Column(length = 3000, nullable = false)
    private String contents;

    @Column(length = 10, nullable = false)
    private String category;

    @Column(length = 500)
    private String thumbnail;

    @Column(length = 1500)
    private String images;

    @Column(nullable = false)
    private int hits;

    @Column(nullable = false)
    private String description;

    public void updateFree(UpdateFreeRequestDto dto, String description, String images) {
        this.title = dto.getTitle();
        this.contents = dto.getContent();
        this.category = dto.getCategory();
        this.thumbnail = dto.getThumbnail();
        this.images = images;
        this.description = description.substring(0, Math.min(description.length(), 100));
    }

    public void increaseHitCount() {
        this.hits++;
    }
}
