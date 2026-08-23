-- 전역 템플릿은 건드리지 않고 yuhyun의 현재 여행 체크리스트만 중복 없이 재구성한다.
USE tripass;

SET @demo_user_id = (
    SELECT id
    FROM users
    WHERE login_provider = 'LOCAL' AND login_id = 'yuhyun'
    LIMIT 1
);

SET @demo_trip_id = (
    SELECT id
    FROM trips
    WHERE user_id = @demo_user_id AND is_deleted = 0
    ORDER BY created_at DESC, id DESC
    LIMIT 1
);

DELETE item
FROM trip_checklist_items item
JOIN trips trip ON trip.id = item.trip_id
WHERE trip.user_id = @demo_user_id;

INSERT INTO trip_checklist_items
    (trip_id, template_id, checklist_type, dday_stage, item_name,
     is_completed, is_excluded, is_custom, is_carried_over, is_deleted)
SELECT
    @demo_trip_id,
    MIN(template.id),
    template.checklist_type,
    template.dday_stage,
    template.item_name,
    0, 0, 0, 0, 0
FROM checklist_templates template
WHERE @demo_trip_id IS NOT NULL
GROUP BY template.checklist_type, template.dday_stage, template.item_name;

SELECT checklist_type, dday_stage, COUNT(*) AS item_count
FROM trip_checklist_items
WHERE trip_id = @demo_trip_id AND is_deleted = 0
GROUP BY checklist_type, dday_stage
ORDER BY checklist_type, dday_stage;
