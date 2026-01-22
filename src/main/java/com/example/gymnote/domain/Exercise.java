package com.example.gymnote.domain;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;

@Entity
@Table(name = "exercises")
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "body_part", columnDefinition = "body_part", nullable = false)
    private BodyPart bodyPart;

    protected Exercise() {
    }

    public Exercise(User user, String name, BodyPart bodyPart) {
        this.user = user;
        this.name = name;
        this.bodyPart = bodyPart;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public BodyPart getBodyPart() {
        return bodyPart;
    }

    public void update(String name, BodyPart bodyPart) {
        this.name = name;
        this.bodyPart = bodyPart;
    }

}
