/* ==================================================================================
   [RESET] 초기화 영역 (에러 방지 필수 코드)
   ================================================================================== */
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE payment_settlement;
TRUNCATE TABLE payment_deposit;
TRUNCATE TABLE payment_transaction;
TRUNCATE TABLE product_image;
TRUNCATE TABLE product_inspection;
TRUNCATE TABLE auction_auctionorder;
TRUNCATE TABLE auction_bid;
TRUNCATE TABLE auction_auction;
TRUNCATE TABLE product_product;
TRUNCATE TABLE payment_wallet;
TRUNCATE TABLE auth_account;
TRUNCATE TABLE product_member;
TRUNCATE TABLE auction_member;
TRUNCATE TABLE payment_member;
TRUNCATE TABLE member_member;

SET FOREIGN_KEY_CHECKS = 1;


/* ==================================================================================
   [STEP 1] 회원(Member) 및 계정(Account/Wallet) 통합 생성
   * User 3: Lego Master (판매자 겸 헤비 콜렉터)
   * User 8: Happy Buyer (주요 구매자)
   ================================================================================== */
-- 1. Member 기본 정보
INSERT INTO member_member (id, deleted, email, nickname, public_id, real_name, contact_phone, address, address_detail, zip_code, intro, created_at, updated_at) VALUES
                                                                                                                                                                    (1,  0, 'system@rare.go',      'system',        '00000000-0000-0000-0000-000000000001', NULL,          NULL,          NULL, NULL, NULL, 'System Administrator', NOW(), NOW()),
                                                                                                                                                                    (2,  0, 'admin@rare.go',       'admin_manager', '00000000-0000-0000-0000-000000000002', '관리자',      '01011112222', '서울 강남구', '테헤란로 123', '06234', 'Main Admin', NOW(), NOW()),
                                                                                                                                                                    (3,  0, 'seller_kim@rare.go',  'lego_master',   '00000000-0000-0000-0000-000000000003', '김판매',      '01033334444', '경기 성남시', '분당구 판교역로', '13494', '레고 전문 판매자', NOW(), NOW()),
                                                                                                                                                                    (4,  0, 'seller_lee@rare.go',  'star_trader',   '00000000-0000-0000-0000-000000000004', '이셀러',      '01055556666', '서울 송파구', '올림픽로 300', '05551', ''스타워즈' 컬렉터', NOW(), NOW()),
                                                                                                                                                                    (5,  0, 'seller_park@rare.go', 'vintage_toy',   '00000000-0000-0000-0000-000000000005', '박상인',      '01077778888', '부산 해운대', '센텀중앙로 10', '48058', '빈티지 토이샵', NOW(), NOW()),
                                                                                                                                                                    (6,  0, 'seller_choi@rare.go', 'figure_shop',   '00000000-0000-0000-0000-000000000006', '최업자',      '01099990000', '대구 수성구', '달구벌대로 500', '42000', '피규어 전문', NOW(), NOW()),
                                                                                                                                                                    (7,  0, 'seller_jung@rare.go', 'brick_world',   '00000000-0000-0000-0000-000000000007', '정대표',      '01012341234', '인천 연수구', '송도과학로 100', '21999', '브릭 월드', NOW(), NOW()),
                                                                                                                                                                    (8,  0, 'user_one@rare.go',    'happy_buyer',   '00000000-0000-0000-0000-000000000008', '홍길동',      '01056785678', '서울 마포구', '양화로 160', '04050', '구매 희망합니다', NOW(), NOW()),
                                                                                                                                                                    (9,  0, 'user_two@rare.go',    'collect_king',  '00000000-0000-0000-0000-000000000009', '김철수',      '01098765432', '대전 서구', '둔산로 100', '35242', '수집광', NOW(), NOW()),
                                                                                                                                                                    (10, 0, 'user_three@rare.go',  'bid_master',    '00000000-0000-0000-0000-000000000010', '이영희',      '01011223344', '광주 서구', '상무대로 777', '61949', '경매 참여자', NOW(), NOW()),
                                                                                                                                                                    (11, 0, 'user_four@rare.go',   'newbie_user',   '00000000-0000-0000-0000-000000000011', '박민수',      '01055667788', '울산 남구', '삼산로 200', '44700', '신규 유저입니다', NOW(), NOW()),
                                                                                                                                                                    (12, 0, 'user_ghost@rare.go',  'ghost_user',    '00000000-0000-0000-0000-000000000012', NULL,          NULL,          NULL, NULL, NULL, '정보 없음', NOW(), NOW());

-- 2. Replica 테이블 동기화 (Product, Auction, Payment 서비스용)
INSERT INTO product_member SELECT * FROM member_member;
INSERT INTO auction_member SELECT * FROM member_member;
INSERT INTO payment_member SELECT * FROM member_member;

-- 3. 소셜 로그인 계정 생성
INSERT INTO auth_account (deleted, member_public_id, provider, provider_id, role, created_at, updated_at) VALUES
                                                                                                              (0, '00000000-0000-0000-0000-000000000001', 'GOOGLE', 'sys_1', 'ADMIN', NOW(), NOW()),
                                                                                                              (0, '00000000-0000-0000-0000-000000000002', 'GOOGLE', 'adm_2', 'ADMIN', NOW(), NOW()),
                                                                                                              (0, '00000000-0000-0000-0000-000000000003', 'KAKAO',  'sel_3', 'SELLER', NOW(), NOW()),
                                                                                                              (0, '00000000-0000-0000-0000-000000000004', 'KAKAO',  'sel_4', 'SELLER', NOW(), NOW()),
                                                                                                              (0, '00000000-0000-0000-0000-000000000005', 'NAVER',  'sel_5', 'SELLER', NOW(), NOW()),
                                                                                                              (0, '00000000-0000-0000-0000-000000000006', 'NAVER',  'sel_6', 'SELLER', NOW(), NOW()),
                                                                                                              (0, '00000000-0000-0000-0000-000000000007', 'GOOGLE', 'sel_7', 'SELLER', NOW(), NOW()),
                                                                                                              (0, '00000000-0000-0000-0000-000000000008', 'KAKAO',  'usr_8', 'USER', NOW(), NOW()),
                                                                                                              (0, '00000000-0000-0000-0000-000000000009', 'KAKAO',  'usr_9', 'USER', NOW(), NOW()),
                                                                                                              (0, '00000000-0000-0000-0000-000000000010', 'NAVER',  'usr_10', 'USER', NOW(), NOW()),
                                                                                                              (0, '00000000-0000-0000-0000-000000000011', 'NAVER',  'usr_11', 'USER', NOW(), NOW()),
                                                                                                              (0, '00000000-0000-0000-0000-000000000012', 'GOOGLE', 'usr_12', 'USER', NOW(), NOW());

-- 4. 지갑 생성 (잔액 0원 초기화)
INSERT INTO payment_wallet (deleted, member_id, balance, holding_amount, created_at, updated_at)
SELECT 0, id, 0, 0, NOW(), NOW() FROM member_member;


/* ==================================================================================
   [STEP 2] 상품(Product) 데이터 생성
   * 태그([1-1] 등) 제거 및 실제 상품명 적용
   * ID 1~8: 검수 대기/거절 (경매 미시작)
   * ID 9~20: 검수 승인 (경매 예정)
   * ID 21~23: 진행 중 (인기)
   * ID 24~29: 종료된 경매 (낙찰, 결제, 정산)
   * ID 30~40: 다양한 시나리오 (스케줄링, 유찰 등)
   ================================================================================== */
INSERT INTO product_product (id, deleted, created_at, updated_at, seller_id, category, name, description, inspection_status, product_condition) VALUES
-- [검수 대기 & 거절]
(1, 0, NOW(), NOW(), 3, '해리포터', '호그와트 성 레고 (미개봉)', '박스 상태 완벽합니다.', 'PENDING', 'INSPECTION'),
(2, 0, NOW(), NOW(), 3, '스타워즈', '밀레니엄 팔콘 UCS', '조립 흔적 없는 새상품입니다.', 'PENDING', 'INSPECTION'),
(3, 0, NOW(), NOW(), 4, '오리지널', '레고 아이디어 볼트론', '1980년대 향수, 작동 확인.', 'PENDING', 'INSPECTION'),
(4, 0, NOW(), NOW(), 4, '스타워즈', '다스베이더 헬멧 피규어', '생활 기스 약간 있음.', 'PENDING', 'INSPECTION'),
(5, 0, NOW(), NOW(), 5, '해리포터', ''해리포터' 호그와트 아이콘', '올리밴더 상점 정품.', 'PENDING', 'INSPECTION'),
(6, 0, NOW(), NOW(), 5, '오리지널', '레고 호환 블럭 (벌크)', '정품 호환 블럭입니다.', 'REJECTED', 'INSPECTION'),
(7, 0, NOW(), NOW(), 6, '스타워즈', '파손된 광선검 핸들', '불은 들어오는데 소리가 안나요.', 'REJECTED', 'INSPECTION'),
(8, 0, NOW(), NOW(), 6, '해리포터', '오래된 도비 피규어', '너무 낡았습니다.', 'REJECTED', 'INSPECTION'),

-- [검수 승인 - 경매 예정]
(9, 0, NOW(), NOW(), 7, '스타워즈', 'R2-D2 UCS 모형', '전시용으로만 사용.', 'APPROVED', 'MISB'),
(10, 0, NOW(), NOW(), 7, '오리지널', '레고 아키텍처 스튜디오', '박스 풀셋.', 'APPROVED', 'MISB'),
(11, 0, NOW(), NOW(), 3, '해리포터', '레고 퀴디치 시합', '부품 누락 없음.', 'APPROVED', 'USED'),
(12, 0, NOW(), NOW(), 3, '스타워즈', 'X-wing 파이터 UCS', '부품 누락 없음.', 'APPROVED', 'NISB'),
(13, 0, NOW(), NOW(), 4, '오리지널', '레고 테크닉 페라리 Daytona', '도색 완성작.', 'APPROVED', 'MISP'),
(14, 0, NOW(), NOW(), 4, '해리포터', '호그와트 기숙사 배너', '유니버셜 스튜디오 정품.', 'APPROVED', 'USED'),
(15, 0, NOW(), NOW(), 5, '스타워즈', '요다 조립식 피규어', '희귀 제품.', 'APPROVED', 'MISB'),
(16, 0, NOW(), NOW(), 5, '오리지널', '레고 닌텐도(NES)', '소장용 S급.', 'APPROVED', 'NISB'),
(17, 0, NOW(), NOW(), 6, '해리포터', '레고 아트: 세계 지도', '인테리어용 최고.', 'APPROVED', 'USED'),
(18, 0, NOW(), NOW(), 6, '스타워즈', '스톰트루퍼 헬멧', '착용 가능.', 'APPROVED', 'USED'),
(19, 0, NOW(), NOW(), 7, '오리지널', '레고 사자 기사의 성', '초대형 제품.', 'APPROVED', 'MISP'),
(20, 0, NOW(), NOW(), 7, '해리포터', '레고 컬렉터 에디션', '실물 사이즈 빗자루 포함.', 'APPROVED', 'USED'),

-- [진행 중 - 인기 경매]
(21, 0, NOW(), NOW(), 3, '스타워즈', '레고 밀레니엄 팔콘', '가장 인기있는 모델.', 'APPROVED', 'MISB'),
(22, 0, NOW(), NOW(), 4, '오리지널', '레고 타이타닉 9000피스', '압도적인 크기.', 'APPROVED', 'NISB'),
(23, 0, NOW(), NOW(), 5, '오리지널', '레고 에펠탑', '전시 효과 최고.', 'APPROVED', 'MISB'),

-- [종료된 경매 - 상태 다양화]
(24, 0, NOW(), NOW(), 6, '오리지널', '레고 갤럭시 익스플로러', 'User 8 낙찰 - 결제 대기중.', 'APPROVED', 'USED'),
(25, 0, NOW(), NOW(), 3, '해리포터', '레고 호그와트 기숙사', 'User 8 낙찰 - 결제 대기중.', 'APPROVED', 'USED'),
(26, 0, NOW(), NOW(), 7, '스타워즈', '레고 데스스타', 'User 8 구매 완료.', 'APPROVED', 'USED'),
(27, 0, NOW(), NOW(), 4, '오리지널', '레고 사자 기사의 성', 'User 3 구매 완료.', 'APPROVED', 'MISB'),
(28, 0, NOW(), NOW(), 5, '스타워즈', '레고 AT-AT', 'User 9 낙찰 후 미결제.', 'APPROVED', 'USED'),
(29, 0, NOW(), NOW(), 6, '해리포터', '레고 다이애건 앨리', 'User 8 구매, 판매자 정산까지 완료.', 'APPROVED', 'MISB'),

-- [기타 시나리오 - 예정/유찰/스케줄링]
(30, 0, NOW(), NOW(), 3, '오리지널', '레고 모듈러 경찰서', '곧 시작합니다.', 'APPROVED', 'MISB'),
(31, 0, NOW(), NOW(), 4, '스타워즈', '레고 배틀팩', '입찰자가 없어 종료됨.', 'APPROVED', 'MISP'),
(32, 0, NOW(), NOW(), 3, '오리지널', '레고 디즈니 캐슬 (71040)', '단종된 희귀 제품.', 'APPROVED', 'MISB'),
(33, 0, NOW(), NOW(), 4, '스타워즈', '레고 테크닉 람보르기니', '전시용으로만 조립.', 'APPROVED', 'USED'),
(34, 0, NOW(), NOW(), 5, '해리포터', '레고 호그와트 익스프레스 (UCS)', '박스 모서리 약간 눌림.', 'APPROVED', 'NISB'),
(35, 0, NOW(), NOW(), 6, '스타워즈', '레고 배틀팩 군단 (50개)', '군단 생성용 일괄 판매.', 'APPROVED', 'MISP'),
(36, 0, NOW(), NOW(), 7, '스타워즈', '레고 AT-AT (UCS)', '초대형 AT-AT.', 'APPROVED', 'USED'),
(37, 0, NOW(), NOW(), 3, '스타워즈', '레고 스타 디스트로이어 (UCS)', '압도적인 크기.', 'APPROVED', 'MISB'),
(38, 0, NOW(), NOW(), 4, '오리지널', '레고 모듈러 경찰서', '단종 직전 모델입니다.', 'APPROVED', 'MISB'),
(39, 0, NOW(), NOW(), 5, '스타워즈', '레고 '스타워즈' AT-TE', '미니피규어 5개 포함.', 'APPROVED', 'USED'),
(40, 0, NOW(), NOW(), 6, '해리포터', '레고 '해리포터' 다이애건 앨리', '확장판 포함 풀세트.', 'APPROVED', 'MISB'),
(41, 0, NOW(), NOW(), 5, '오리지널', '레고 시티 경찰서', '부품 누락 없는 S급.', 'APPROVED', 'USED'),
(42, 0, NOW(), NOW(), 3, '스타워즈', '레고 테크닉 부가티', '낙찰자가 도망간 매물입니다.', 'APPROVED', 'MISB'),
(43, 0, NOW(), NOW(), 4, '오리지널', '레고 꽃다발', '선물용으로 좋습니다.', 'APPROVED', 'NISB');


/* ==================================================================================
   [STEP 3] 상품 이미지 매핑 (Unsplash Real URLs)
   ================================================================================== */
INSERT INTO product_image (deleted, created_at, updated_at, product_id, image_url, sort_order) VALUES
                                                                                                   (0, NOW(), NOW(), 1, 'https://images.unsplash.com/photo-1611604548018-d56bbd85d681?q=80&w=2070&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 2, 'https://images.unsplash.com/photo-1610483178766-8092d96033f3?q=80&w=987&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 3, 'https://images.unsplash.com/photo-1505322033502-1f4385692e6a?q=80&w=1968&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 4, 'https://images.unsplash.com/photo-1566207962290-9e3cc51f4199?q=80&w=1963&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 5, 'https://images.unsplash.com/photo-1739800920955-c0c27622f9b3?q=80&w=987&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 6, 'https://images.unsplash.com/photo-1643921185080-1b875514cd00?q=80&w=987&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 7, 'https://images.unsplash.com/flagged/photo-1558706379-e9698f05d675?q=80&w=2023&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 8, 'https://images.unsplash.com/photo-1607297737950-b3c024a71a69?q=80&w=2071&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 9, 'https://images.unsplash.com/photo-1558492426-df14e290aefa?q=80&w=2070&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 10, 'https://images.unsplash.com/photo-1643921160200-feed3b6b2c79?q=80&w=987&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 11, 'https://images.unsplash.com/photo-1609741200064-2ef87d5eb200?q=80&w=2070&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 12, 'https://images.unsplash.com/photo-1628868755645-08e2102c8b14?q=80&w=2070&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 13, 'https://images.unsplash.com/photo-1647080203535-8610f7971802?q=80&w=1064&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 14, 'https://images.unsplash.com/photo-1605012464390-45820d1f7bdf?q=80&w=2070&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 15, 'https://images.unsplash.com/photo-1728550958364-1e0348a09508?q=80&w=2067&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 16, 'https://images.unsplash.com/photo-1743383085795-15138e0b83a4?q=80&w=987&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 17, 'https://images.unsplash.com/photo-1640248471910-4a66d9f8b400?q=80&w=2070&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 18, 'https://images.unsplash.com/photo-1681415943601-c27088f8c004?q=80&w=987&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 19, 'https://images.unsplash.com/photo-1611347022310-af47588f6aba?q=80&w=2076&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 20, 'https://images.unsplash.com/photo-1609283040241-7a50dc4288bf?q=80&w=987&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 21, 'https://images.unsplash.com/photo-1610383547060-7b049ea45e9e?q=80&w=2074&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 22, 'https://images.unsplash.com/photo-1708232966788-0a8a02acda7b?q=80&w=987&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 23, 'https://images.unsplash.com/photo-1636838258291-104f8399c2f6?q=80&w=1004&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 24, 'https://images.unsplash.com/photo-1627895766710-7cf5c594f08e?q=80&w=987&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 25, 'https://images.unsplash.com/photo-1638233005583-db9a4187a3b7?q=80&w=2074&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 26, 'https://images.unsplash.com/photo-1594736797933-d0501ba2fe65?q=80&w=1034&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 27, 'https://images.unsplash.com/photo-1610213881011-ba006d40d5b4?q=80&w=2070&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 28, 'https://images.unsplash.com/photo-1581343979186-ed71e8b09d44?q=80&w=1974&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 29, 'https://images.unsplash.com/photo-1621446484696-7bebf3c9e730?q=80&w=987&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 30, 'https://images.unsplash.com/photo-1671043120530-930359cee399?q=80&w=988&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 31, 'https://images.unsplash.com/photo-1647079997706-2b1e2739b79d?q=80&w=1064&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
-- 이미지 재사용 (32번부터 다시 1번 이미지 사용)
                                                                                                   (0, NOW(), NOW(), 32, 'https://images.unsplash.com/photo-1611604548018-d56bbd85d681?q=80&w=2070&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 33, 'https://images.unsplash.com/photo-1610483178766-8092d96033f3?q=80&w=987&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 34, 'https://images.unsplash.com/photo-1505322033502-1f4385692e6a?q=80&w=1968&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 35, 'https://images.unsplash.com/photo-1593501828586-b07c7fa6cb5d?q=80&w=2070&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 36, 'https://images.unsplash.com/photo-1741745773415-f35a150686ab?q=80&w=2548&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 37, 'https://images.unsplash.com/photo-1643921185080-1b875514cd00?q=80&w=987&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 38, 'https://images.unsplash.com/flagged/photo-1558706379-e9698f05d675?q=80&w=2023&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 39, 'https://images.unsplash.com/photo-1607297737950-b3c024a71a69?q=80&w=2071&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 40, 'https://images.unsplash.com/photo-1558492426-df14e290aefa?q=80&w=2070&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 41, 'https://images.unsplash.com/photo-1643921160200-feed3b6b2c79?q=80&w=987&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 42, 'https://images.unsplash.com/photo-1609741200064-2ef87d5eb200?q=80&w=2070&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0),
                                                                                                   (0, NOW(), NOW(), 43, 'https://images.unsplash.com/photo-1628868755645-08e2102c8b14?q=80&w=2070&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D', 0);


/* ==================================================================================
   [STEP 6] PRODUCT_INSPECTION 생성 (검수 상태)
   ================================================================================== */
INSERT INTO product_inspection (deleted, created_at, updated_at, seller_id, product_id, inspector_id, inspection_status, product_condition, reason)
SELECT 0, NOW(), NOW(), p.seller_id, p.id, 2,
       p.inspection_status,
       CASE WHEN p.inspection_status = 'REJECTED' THEN 'MISB' ELSE p.product_condition END,
       CASE WHEN p.inspection_status = 'REJECTED' THEN '검수 부적합' ELSE NULL END
FROM product_product p;


/* ==================================================================================
   [STEP 7] AUCTION_AUCTION 생성 (경매 상태)
   * 상태별로 그룹화하여 데이터 삽입
   ================================================================================== */
INSERT INTO auction_auction (id, deleted, created_at, updated_at, product_id, seller_id, duration_days, start_price, tick_size, status, start_time, end_time, current_price) VALUES
-- [PENDING] 검수 대기/미시작
(1, 0, NOW(), NOW(), 1, 3, 7, 500000, 10000, 'SCHEDULED', NULL, NULL, NULL),
(2, 0, NOW(), NOW(), 2, 3, 5, 1200000, 30000, 'SCHEDULED', NULL, NULL, NULL),
(3, 0, NOW(), NOW(), 3, 4, 3, 80000, 2000, 'SCHEDULED', NULL, NULL, NULL),
(4, 0, NOW(), NOW(), 4, 4, 3, 150000, 5000, 'SCHEDULED', NULL, NULL, NULL),
(5, 0, NOW(), NOW(), 5, 5, 7, 45000, 1000, 'SCHEDULED', NULL, NULL, NULL),

-- [WITHDRAWN] 검수 거절
(6, 0, NOW(), NOW(), 6, 5, 3, 10000, 1000, 'WITHDRAWN', NULL, NULL, NULL),
(7, 0, NOW(), NOW(), 7, 6, 3, 20000, 1000, 'WITHDRAWN', NULL, NULL, NULL),
(8, 0, NOW(), NOW(), 8, 6, 3, 5000, 500, 'WITHDRAWN', NULL, NULL, NULL),

-- [SCHEDULED] 오픈 예정
(9, 0, NOW(), NOW(), 9, 7, 7, 3500000, 30000, 'SCHEDULED', DATE_ADD(NOW(), INTERVAL 10 MINUTE), DATE_ADD(NOW(), INTERVAL 7 DAY), NULL),
(10, 0, NOW(), NOW(), 10, 7, 5, 800000, 10000, 'SCHEDULED', DATE_ADD(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 5 DAY), NULL),
(11, 0, NOW(), NOW(), 11, 3, 3, 60000, 2000, 'SCHEDULED', DATE_ADD(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 3 DAY), NULL),
(12, 0, NOW(), NOW(), 12, 3, 5, 250000, 5000, 'SCHEDULED', DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 6 DAY), NULL),
(13, 0, NOW(), NOW(), 13, 4, 7, 400000, 10000, 'SCHEDULED', DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 10 DAY), NULL),
(14, 0, NOW(), NOW(), 14, 4, 3, 30000, 1000, 'SCHEDULED', DATE_ADD(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 10 DAY), NULL),
(15, 0, NOW(), NOW(), 15, 5, 5, 550000, 10000, 'SCHEDULED', NULL, NULL, NULL),
(16, 0, NOW(), NOW(), 16, 5, 7, 1500000, 30000, 'SCHEDULED', NULL, NULL, NULL),
(17, 0, NOW(), NOW(), 17, 6, 3, 45000, 1000, 'SCHEDULED', NULL, NULL, NULL),
(18, 0, NOW(), NOW(), 18, 6, 5, 120000, 5000, 'SCHEDULED', NULL, NULL, NULL),
(19, 0, NOW(), NOW(), 19, 7, 7, 900000, 10000, 'SCHEDULED', NULL, NULL, NULL),
(20, 0, NOW(), NOW(), 20, 7, 5, 380000, 10000, 'SCHEDULED', NULL, NULL, NULL),

-- [IN_PROGRESS] 진행 중
(21, 0, NOW(), NOW(), 21, 3, 3, 100000, 2000, 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), 150000),
(22, 0, NOW(), NOW(), 22, 4, 7, 500000, 10000, 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY), 550000),
(23, 0, NOW(), NOW(), 23, 5, 1, 200000, 5000, 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 23 HOUR), DATE_ADD(NOW(), INTERVAL 10 MINUTE), 220000),
(27, 0, NOW(), NOW(), 27, 4, 1, 40000, 1000, 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 60 MINUTE), DATE_ADD(NOW(), INTERVAL 1 MINUTE), 45000),
(28, 0, NOW(), NOW(), 28, 5, 1, 50000, 1000, 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 60 MINUTE), DATE_ADD(NOW(), INTERVAL 5 MINUTE), 55000),
(29, 0, NOW(), NOW(), 29, 6, 1, 10000, 1000, 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 60 MINUTE), DATE_ADD(NOW(), INTERVAL 10 MINUTE), 10000),
(32, 0, NOW(), NOW(), 32, 3, 1, 150000, 5000, 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(DATE_SUB(NOW(), INTERVAL 1 HOUR), INTERVAL 1 DAY), 150000),
(33, 0, NOW(), NOW(), 33, 4, 3, 300000, 10000, 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 24 HOUR), DATE_ADD(DATE_SUB(NOW(), INTERVAL 24 HOUR), INTERVAL 3 DAY), 320000),
(34, 0, NOW(), NOW(), 34, 5, 5, 250000, 5000, 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(DATE_SUB(NOW(), INTERVAL 2 HOUR), INTERVAL 5 DAY), 255000),
(35, 0, NOW(), NOW(), 35, 6, 7, 50000, 2000, 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 3 DAY), INTERVAL 7 DAY), 82000),
(36, 0, NOW(), NOW(), 36, 7, 3, 1200000, 30000, 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 2 DAY), INTERVAL 3 DAY), 1350000),
(37, 0, NOW(), NOW(), 37, 3, 7, 3500000, 30000, 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 10 MINUTE), DATE_ADD(DATE_SUB(NOW(), INTERVAL 10 MINUTE), INTERVAL 7 DAY), 3600000),

-- [ENDED] 종료 및 낙찰
(24, 0, NOW(), NOW(), 24, 6, 3, 50000, 1000, 'ENDED', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 80000),
(25, 0, NOW(), NOW(), 25, 3, 3, 30000, 1000, 'ENDED', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 1 HOUR), 45000),
(26, 0, NOW(), NOW(), 26, 7, 5, 300000, 5000, 'ENDED', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), 400000),
(30, 0, NOW(), NOW(), 30, 3, 1, 500000, 10000, 'ENDED', DATE_SUB(NOW(), INTERVAL 300 MINUTE), DATE_SUB(NOW(), INTERVAL 60 MINUTE), 600000),
(31, 0, NOW(), NOW(), 31, 4, 1, 200000, 5000, 'ENDED', DATE_SUB(NOW(), INTERVAL 300 MINUTE), DATE_SUB(NOW(), INTERVAL 60 MINUTE), 250000),
(41, 0, NOW(), NOW(), 41, 5, 3, 150000, 5000, 'ENDED', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 180000),
(42, 0, NOW(), NOW(), 42, 3, 5, 400000, 10000, 'ENDED', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 450000),
(43, 0, NOW(), NOW(), 43, 4, 3, 50000, 2000, 'ENDED', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY), 70000),

-- [ENDED & FAILED] 결제 실패/유찰
(38, 0, NOW(), NOW(), 38, 4, 3, 10000, 1000, 'WITHDRAWN', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 10000), -- 유찰로 수정
(39, 0, NOW(), NOW(), 39, 5, 3, 300000, 5000, 'SCHEDULED', DATE_ADD(NOW(), INTERVAL 10 MINUTE), DATE_ADD(NOW(), INTERVAL 3 DAY), NULL), -- 예정
(40, 0, NOW(), NOW(), 40, 6, 5, 150000, 5000, 'SCHEDULED', DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 6 DAY), NULL); -- 예정


/* ==================================================================================
   [STEP 8] AUCTION_BID 데이터 (입찰 내역)
   ================================================================================== */
INSERT INTO auction_bid (deleted, created_at, updated_at, auction_id, bid_amount, bid_time, bidder_id) VALUES
-- 21 (팔콘)
(0, NOW(), NOW(), 21, 110000, DATE_SUB(NOW(), INTERVAL 20 HOUR), 8),
(0, NOW(), NOW(), 21, 120000, DATE_SUB(NOW(), INTERVAL 10 HOUR), 9),
(0, NOW(), NOW(), 21, 150000, DATE_SUB(NOW(), INTERVAL 1 HOUR), 8),
-- 22 (타이타닉)
(0, NOW(), NOW(), 22, 520000, DATE_SUB(NOW(), INTERVAL 1 DAY), 3),
(0, NOW(), NOW(), 22, 550000, DATE_SUB(NOW(), INTERVAL 12 HOUR), 8),
-- 23 (에펠탑)
(0, NOW(), NOW(), 23, 210000, DATE_SUB(NOW(), INTERVAL 2 HOUR), 9),
(0, NOW(), NOW(), 23, 220000, DATE_SUB(NOW(), INTERVAL 1 HOUR), 3),
-- 24 (갤럭시)
(0, NOW(), NOW(), 24, 60000, DATE_SUB(NOW(), INTERVAL 2 DAY), 8),
(0, NOW(), NOW(), 24, 80000, DATE_SUB(NOW(), INTERVAL 1 DAY), 8),
-- 25 (호그와트 기숙사)
(0, NOW(), NOW(), 25, 40000, DATE_SUB(NOW(), INTERVAL 2 DAY), 8),
(0, NOW(), NOW(), 25, 45000, DATE_SUB(NOW(), INTERVAL 2 HOUR), 8),
-- 26 (데스스타)
(0, NOW(), NOW(), 26, 400000, DATE_SUB(NOW(), INTERVAL 6 DAY), 8),
-- 30 (호그와트 열차)
(0, NOW(), NOW(), 30, 600000, DATE_SUB(NOW(), INTERVAL 2 HOUR), 8),
-- 31 (다이애건 앨리)
(0, NOW(), NOW(), 31, 250000, DATE_SUB(NOW(), INTERVAL 2 HOUR), 9),
-- 33 (람보르기니)
(0, NOW(), NOW(), 33, 310000, DATE_SUB(NOW(), INTERVAL 20 HOUR), 8),
(0, NOW(), NOW(), 33, 320000, DATE_SUB(NOW(), INTERVAL 10 HOUR), 9),
-- 34 (호그와트 익스프레스)
(0, NOW(), NOW(), 34, 255000, DATE_SUB(NOW(), INTERVAL 1 HOUR), 10),
-- 35 (배틀팩)
(0, NOW(), NOW(), 35, 55000, DATE_SUB(NOW(), INTERVAL 2 DAY), 11),
(0, NOW(), NOW(), 35, 65000, DATE_SUB(NOW(), INTERVAL 1 DAY), 3),
(0, NOW(), NOW(), 35, 75000, DATE_SUB(NOW(), INTERVAL 12 HOUR), 11),
(0, NOW(), NOW(), 35, 80000, DATE_SUB(NOW(), INTERVAL 6 HOUR), 4),
(0, NOW(), NOW(), 35, 82000, DATE_SUB(NOW(), INTERVAL 1 HOUR), 12),
-- 36 (AT-AT)
(0, NOW(), NOW(), 36, 1230000, DATE_SUB(NOW(), INTERVAL 40 HOUR), 9),
(0, NOW(), NOW(), 36, 1260000, DATE_SUB(NOW(), INTERVAL 30 HOUR), 10),
(0, NOW(), NOW(), 36, 1300000, DATE_SUB(NOW(), INTERVAL 10 HOUR), 9),
(0, NOW(), NOW(), 36, 1350000, DATE_SUB(NOW(), INTERVAL 1 HOUR), 12),
-- 37 (스타 디스트로이어)
(0, NOW(), NOW(), 37, 3530000, DATE_SUB(NOW(), INTERVAL 8 MINUTE), 6),
(0, NOW(), NOW(), 37, 3560000, DATE_SUB(NOW(), INTERVAL 5 MINUTE), 9),
(0, NOW(), NOW(), 37, 3600000, DATE_SUB(NOW(), INTERVAL 2 MINUTE), 7),
-- 41 (시티 경찰서)
(0, NOW(), NOW(), 41, 155000, DATE_SUB(NOW(), INTERVAL 3 DAY), 8),
(0, NOW(), NOW(), 41, 160000, DATE_SUB(NOW(), INTERVAL 50 HOUR), 10),
(0, NOW(), NOW(), 41, 180000, DATE_SUB(NOW(), INTERVAL 49 HOUR), 8),
-- 42 (테크닉 부가티)
(0, NOW(), NOW(), 42, 410000, DATE_SUB(NOW(), INTERVAL 4 DAY), 9),
(0, NOW(), NOW(), 42, 450000, DATE_SUB(NOW(), INTERVAL 3 DAY), 11),
-- 43 (꽃다발)
(0, NOW(), NOW(), 43, 70000, DATE_SUB(NOW(), INTERVAL 8 DAY), 12);


/* ==================================================================================
   [STEP 9] PAYMENT_DEPOSIT 생성 (보증금 상태)
   ================================================================================== */
INSERT INTO payment_deposit (deleted, created_at, updated_at, member_id, auction_id, amount, status) VALUES
-- 21: 진행중 (HOLD)
(0, NOW(), NOW(), 8, 21, 10000, 'HOLD'),
(0, NOW(), NOW(), 9, 21, 10000, 'HOLD'),
-- 22: 진행중 (HOLD)
(0, NOW(), NOW(), 3, 22, 50000, 'HOLD'),
(0, NOW(), NOW(), 8, 22, 50000, 'HOLD'),
-- 23: 진행중 (HOLD)
(0, NOW(), NOW(), 9, 23, 20000, 'HOLD'),
(0, NOW(), NOW(), 3, 23, 20000, 'HOLD'),
-- 24: 결제대기 (8: HOLD, 9: RELEASED)
(0, NOW(), NOW(), 8, 24, 5000, 'HOLD'),
(0, NOW(), NOW(), 9, 24, 5000, 'RELEASED'), -- 정정: 패찰자는 환불됨
-- 25: 결제대기 (3: HOLD)
(0, NOW(), NOW(), 8, 25, 3000, 'HOLD'),
-- 26: 결제완료 (8: USED)
(0, NOW(), NOW(), 8, 26, 30000, 'USED'),
-- 30: 결제완료 (8: USED)
(0, NOW(), NOW(), 8, 30, 50000, 'USED'),
-- 31: 결제실패 (9: FORFEITED)
(0, NOW(), NOW(), 9, 31, 20000, 'FORFEITED'),
-- 41: 결제완료 (8: USED, 10: RELEASED)
(0, NOW(), NOW(), 8, 41, 15000, 'USED'),
(0, NOW(), NOW(), 10, 41, 15000, 'RELEASED'),
-- 42: 결제실패 (11: FORFEITED, 9: RELEASED)
(0, NOW(), NOW(), 11, 42, 40000, 'FORFEITED'),
(0, NOW(), NOW(), 9, 42, 40000, 'RELEASED'),
-- 43: 결제완료 (12: USED)
(0, NOW(), NOW(), 12, 43, 5000, 'USED'),
-- 35: 진행중 (11, 3, 4, 12 HOLD)
(0, NOW(), NOW(), 11, 35, 5000, 'HOLD'),
(0, NOW(), NOW(), 3, 35, 5000, 'HOLD'),
(0, NOW(), NOW(), 4, 35, 5000, 'HOLD'),
(0, NOW(), NOW(), 12, 35, 5000, 'HOLD'),
-- 36: 진행중 (9, 10, 12 HOLD)
(0, NOW(), NOW(), 9, 36, 120000, 'HOLD'),
(0, NOW(), NOW(), 10, 36, 120000, 'HOLD'),
(0, NOW(), NOW(), 12, 36, 120000, 'HOLD'),
-- 37: 진행중 (6, 9, 7 HOLD)
(0, NOW(), NOW(), 6, 37, 350000, 'HOLD'),
(0, NOW(), NOW(), 9, 37, 350000, 'HOLD'),
(0, NOW(), NOW(), 7, 37, 350000, 'HOLD');


/* ==================================================================================
   [STEP 10] AUCTION_AUCTIONORDER 생성 (주문 상태)
   ================================================================================== */
INSERT INTO auction_auctionorder (deleted, created_at, updated_at, auction_id, bidder_id, final_price, seller_id, status) VALUES
                                                                                                                              (0, NOW(), NOW(), 24, 8, 80000, 6, 'PROCESSING'),
(0, NOW(), NOW(), 25, 8, 45000, 3, 'PROCESSING'),
                                                                                                                              (0, NOW(), NOW(), 26, 8, 400000, 7, 'SUCCESS'),
                                                                                                                              (0, NOW(), NOW(), 30, 8, 600000, 3, 'SUCCESS'),
                                                                                                                              (0, NOW(), NOW(), 31, 9, 250000, 4, 'FAILED'),
                                                                                                                              (0, NOW(), NOW(), 41, 8, 180000, 5, 'SUCCESS'),
                                                                                                                              (0, NOW(), NOW(), 42, 11, 450000, 3, 'FAILED'),
                                                                                                                              (0, NOW(), NOW(), 43, 12, 70000, 4, 'SUCCESS');


/* ==================================================================================
   [STEP 11] PAYMENT_SETTLEMENT 생성 (정산 상태)
   ================================================================================== */
INSERT INTO payment_settlement (deleted, created_at, updated_at, auction_id, seller_id, sales_amount, fee_amount, settlement_amount, status, try_count) VALUES
                                                                                                                                                 (0, NOW(), NOW(), 26, 7, 400000, 40000, 360000, 'READY', 0),
                                                                                                                                                 (0, NOW(), NOW(), 30, 3, 600000, 60000, 540000, 'READY', 0),
                                                                                                                                                 (0, NOW(), NOW(), 31, 4, 20000, 0, 20000, 'READY', 0), -- 몰수금 정산
                                                                                                                                                 (0, NOW(), NOW(), 41, 5, 180000, 18000, 162000, 'READY', 0),
                                                                                                                                                 (0, NOW(), NOW(), 42, 3, 40000, 0, 40000, 'READY', 0), -- 몰수금 정산
                                                                                                                                                 (0, NOW(), NOW(), 43, 4, 70000, 7000, 63000, 'DONE', 0);


/* ==================================================================================
   [STEP 12] 보증금 및 잔액 재계산 (최종)
   ================================================================================== */
-- 1. Holding Amount 초기화
UPDATE payment_wallet SET holding_amount = 0;

-- 2. Holding Amount 재계산 (HOLD 상태인 Deposit 합계)
UPDATE payment_wallet w
    JOIN (
        SELECT member_id, SUM(amount) as total_hold
        FROM payment_deposit
        WHERE status = 'HOLD'
        GROUP BY member_id
    ) d ON w.member_id = d.member_id
SET w.holding_amount = d.total_hold;

-- 3. Balance 보정 (기본금 + Holding보다 많게)
UPDATE payment_wallet
SET balance = holding_amount + 10000000
WHERE balance < holding_amount;
