package com.example.postservice.service;

import com.example.postservice.entity.Post;
import com.example.postservice.entity.Reaction;
import com.example.postservice.entity.User;
import com.example.postservice.enums.ReactionType;
import com.example.postservice.repository.ReactionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReactionService {

    private final ReactionRepo reactionRepo;

    public void reactToPost(Post post, User user, ReactionType type) {
        Reaction existing = reactionRepo.findByPostAndUser(post, user).orElse(null);

        if (existing != null) {
            if (existing.getType() == type) {
                reactionRepo.delete(existing); // toggle
            } else {
                existing.setType(type); // change reaction
                reactionRepo.save(existing);
            }
        } else {
            Reaction reaction = new Reaction();
            reaction.setPost(post);
            reaction.setUser(user);
            reaction.setType(type);
            reactionRepo.save(reaction);
        }
    }

    public Map<ReactionType, Long> getReactionCounts(Post post) {
        Map<ReactionType, Long> map = new EnumMap<>(ReactionType.class);
        for (ReactionType type : ReactionType.values()) {
            map.put(type, reactionRepo.countByPostAndType(post, type));
        }
        return map;
    }

}
