```sql
-- 事件发生次数条件查询
select
    deviceId,
    count(1) as cnt
from event_detail
where deviceId='nJBTQejDxDmc' and eventId='adShow' and properties['adId']='14'
  and timeStamp between 1615900460000 and 1615900580000
group by deviceId
;

-- 事件序列条件查询
SELECT
    deviceId,
    sequenceMatch('.*(?1).*(?2).*(?3)')(
    toDateTime(`timeStamp`),
    eventId = 'adShow' and properties['adId']='10',
    eventId = 'addCart' and properties['pageId']='720',
    eventId = 'collect' and properties['pageId']='263'
  ) as is_match3,
        sequenceMatch('.*(?1).*(?2)')(
    toDateTime(`timeStamp`),
    eventId = 'adShow' and properties['adId']='10',
    eventId = 'addCart' and properties['pageId']='720'
  ) as is_match2,
        sequenceMatch('.*(?1).*')(
    toDateTime(`timeStamp`),
    eventId = 'adShow' and properties['adId']='10',
    eventId = 'addCart' and properties['pageId']='720'
  ) as is_match1
from event_detail
where deviceId='rVacGhu7OJgl' and  `timeStamp` > 1615900460000
  and (
        (eventId='adShow' and properties['adId']='10')
        or
        (eventId = 'addCart' and properties['pageId']='720')
        or
        (eventId = 'collect' and properties['pageId']='263')
    )
group by deviceId
;

/*
┌─deviceId─────┬─is_match3─┬─is_match2─┬─is_match1─┐
│ rVacGhu7OJgl │         1 │         1 │         1 │
└──────────────┴───────────┴───────────┴───────────┘

*/
```