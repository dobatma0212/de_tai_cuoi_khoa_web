package com.storechain.group4.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Ảnh/video minh họa kèm theo một đánh giá sản phẩm.
 */
@Entity
@Table(name = "review_media")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "media_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private ProductReview review;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    /** IMAGE hoặc VIDEO */
    @Column(name = "media_type", nullable = false, length = 20)
    @Builder.Default
    private String mediaType = "IMAGE";
}
