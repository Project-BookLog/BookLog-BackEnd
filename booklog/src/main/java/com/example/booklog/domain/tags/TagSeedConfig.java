package com.example.booklog.domain.tags.seed;

import com.example.booklog.domain.tags.entity.TagCategory;
import com.example.booklog.domain.tags.entity.Tags;
import com.example.booklog.domain.tags.repository.TagsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class TagSeedConfig {

    private final TagsRepository tagsRepository;

    @Bean
    public ApplicationRunner tagSeeder() {
        return args -> seed();
    }

    @Transactional
    public void seed() {
        List<Tags> seeds = List.of(
                // MOOD
                Tags.builder().category(TagCategory.MOOD).name("따뜻한").build(),
                Tags.builder().category(TagCategory.MOOD).name("잔잔한").build(),
                Tags.builder().category(TagCategory.MOOD).name("서늘한").build(),
                Tags.builder().category(TagCategory.MOOD).name("몽환적인").build(),
                Tags.builder().category(TagCategory.MOOD).name("유쾌한").build(),

                // IMMERSION
                Tags.builder().category(TagCategory.IMMERSION).name("기분전환").build(),
                Tags.builder().category(TagCategory.IMMERSION).name("지적인 탐구").build(),
                Tags.builder().category(TagCategory.IMMERSION).name("압도적 몰입").build(),
                Tags.builder().category(TagCategory.IMMERSION).name("짙은 여운").build(),

                // STYLE
                Tags.builder().category(TagCategory.STYLE).name("간결한").build(),
                Tags.builder().category(TagCategory.STYLE).name("화려한").build(),
                Tags.builder().category(TagCategory.STYLE).name("담백한").build(),
                Tags.builder().category(TagCategory.STYLE).name("섬세한").build(),
                Tags.builder().category(TagCategory.STYLE).name("직설적").build(),
                Tags.builder().category(TagCategory.STYLE).name("은유적").build()
        );

        for (Tags t : seeds) {
            if (!tagsRepository.existsByCategoryAndName(t.getCategory(), t.getName())) {
                tagsRepository.save(t);
            }
        }
    }
}
