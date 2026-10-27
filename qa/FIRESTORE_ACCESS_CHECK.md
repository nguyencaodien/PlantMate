# Firestore access check — issues #34 / #43

Ngày kiểm tra: **27/10/2026**, múi giờ Asia/Saigon. Người được giao: `pxshyn`.
Nguồn kiểm tra: bản Android đang mở trên ổ D:, được người dùng xác nhận sử dụng.
Không có `.git` trong bản local để xác định commit nền. `main` của TreeShare tại
`82322019cb849bda87e0ac3488b6c89beef918ec` chưa chứa ứng dụng Android. Bốn file
nộp cần được tích hợp vào bản nền Android do nhóm thống nhất trước khi merge;
bản nộp riêng lẻ không phải project có thể build từ GitHub.

## Thay đổi #34

- `users/{uid}` create gọi `validAvatar`, thống nhất định dạng và quyền sở hữu
  với avatar update. Cho phép rỗng, preset đúng cú pháp, ảnh Firestore của chính
  người tạo; từ chối URL/URI hoặc ảnh của người khác.
- `chatMedia` create kiểm tra block theo cả hai chiều từ hai thành viên trong
  conversation. Quyền đọc lịch sử, sửa/xóa media và các giới hạn hiện có giữ nguyên.
- Không sửa `firestore-tests/rules.test.mjs` hoặc `firestore-tests/run.mjs`.
  Không deploy rules, không dùng Firebase production hoặc tài khoản thật.

## Kết quả đã chạy

| Kiểm tra | Kết quả | Phạm vi chứng minh |
|---|---|---|
| Bộ rules hiện có, Firestore Emulator `demo-plantmate` | **45/45 PASS**, 13 suites, không skip | Hồi quy các quyền hiện có. |
| Fixture bổ sung ở cuối tài liệu | **22/22 PASS**, không skip | 18 ca avatar/media, 4 ca giao thức batch admin. |
| `:app:compileDebugJavaWithJavac` | **PASS** | Mã Android hiện có cùng thay đổi #43 biên dịch thành công. |
| `:app:testDebugUnitTest --tests '*FirestoreAdminRulesTest'` | **5/5 PASS**, không skip | Các assertion Java hiện có về quyền admin/audit; đây là kiểm tra tĩnh rules. |
| Đối chiếu #42 | Đã đọc model, Room v10/migrations, repository và rules | Không chạy migration/CRUD Room vì không thay đổi Room. |

Gradle: **BUILD SUCCESSFUL**, 26 tasks (15 executed, 11 up-to-date).
Không khẳng định đã chạy toàn bộ unit suite, instrumentation Android, UI hay
Firebase production. Các fixture batch dùng SDK JavaScript để kiểm tra giao
thức xóa trên emulator, **không trực tiếp thực thi lớp Java Android**.

## Ma trận fixture

| Tình huống | Kết quả mong đợi và đã quan sát |
|---|---|
| Tạo profile với avatar rỗng / preset / ảnh của mình | ALLOW cả 3. |
| Ảnh người khác / ảnh thiếu / HTTP URL / local URI / preset sai / ref sai / số | DENY cả 7. |
| Update text giữ avatar legacy; đổi thành ảnh người khác | ALLOW giữ nguyên, DENY thay trái quyền. |
| IMAGE và VOICE; không block | Cả hai thành viên được upload. |
| IMAGE và VOICE; Alice block Bob hoặc Bob block Alice | Cả hai bị từ chối upload; vẫn đọc media/message lịch sử; bỏ block thì upload lại được. |
| Outsider / signed-out / suspended | Upload DENY; outsider đọc media DENY. |
| Admin xóa 810 comments/likes | Batch 400, 400, 10; cuối cùng 2 writes parent + audit. Không còn con/parent; audit tồn tại. |
| Admin xóa 805 replies | Batch 400, 400, 5, rồi root + audit; post vẫn tồn tại. |
| Thu hồi quyền admin sau batch đầu | Trả lỗi, còn 410 con và parent, chưa có audit hoàn tất. Khôi phục quyền và retry: 400, 10, 2. |
| Audit cuối sai adminId | Batch parent/audit thất bại nguyên tử; parent còn, không có audit. Retry hợp lệ với 0 con chỉ cần 2 writes. |

## Giới hạn #43

`FirebaseAdminDataSource` truy vấn con bằng `Source.SERVER`, dùng cùng giới hạn
400 và helper chia batch của `FirebaseAccountDataSource`, chờ từng commit;
lỗi truy vấn/batch được trả về và ngăn bước tiếp theo. Parent + audit cùng batch
cuối. Retry truy vấn lại phần còn lại trên server, không dùng danh sách cache.

Đây không phải giao dịch nguyên tử cho toàn bộ cây: các batch đã thành công
không rollback. Truy vấn ban đầu vẫn giữ các references trong bộ nhớ và không
khóa việc người khác thêm con trong lúc xóa. Không bổ sung tombstone, backend
lock hoặc schema mới trong issue này. Chưa kiểm tra đồng thời tạo con trên
Android; nhóm cần đánh giá riêng nếu yêu cầu bảo đảm không có orphan khi cạnh tranh.

## Chạy lại

Cần bản nền Android đầy đủ và dependencies sẵn có trong `firestore-tests`.
Thực hiện tại gốc ứng dụng; chỉ dùng project `demo-plantmate`.

```powershell
# Bộ rules có sẵn (chạy trong firestore-tests):
node node_modules/firebase-tools/lib/bin/firebase.js emulators:exec --only firestore --project demo-plantmate "node --test rules.test.mjs"

# Lưu khối JavaScript bên dưới vào build/issue-submission/access-fixtures.mjs,
# rồi chạy từ firestore-tests:
node node_modules/firebase-tools/lib/bin/firebase.js emulators:exec --only firestore --project demo-plantmate "node --test ../build/issue-submission/access-fixtures.mjs"

# Kiểm tra Android, chạy từ gốc project:
.\gradlew.bat :app:compileDebugJavaWithJavac :app:testDebugUnitTest --tests '*FirestoreAdminRulesTest' --console=plain
```

Máy kiểm tra thiếu `npm`/`npx` trên PATH nên gọi Firebase CLI đã cài bằng `node`;
không chỉnh runner hoặc cài dependency mới. Emulator dùng JBR 21.0.11; Gradle
dùng JBR 25.0.3 có sẵn, đúng daemon toolchain 25 của project; Java source/target
vẫn là 11. Cả hai dùng thư mục tạm `build/tmp`. Sandbox ban đầu không mở được
localhost/cache; các lần PASS chạy ngoài sandbox với quyền đã được cấp.
JBR 25 với TEMP dài từng báo lỗi loopback; chọn TEMP ngắn khắc phục. Lần Gradle
dùng JBR 21 từng bắt đầu tải toolchain 25; kết quả PASS dùng JBR 25 có sẵn.

## Dấu vết file

SHA-256 tính trên byte file local; chỉ nhằm nhận diện bản đã kiểm tra.

| File | SHA-256 trước sửa | SHA-256 đã kiểm tra |
|---|---|---|
| `firestore.rules` | `74e7f1091b5b832274bfd174b3bcde7f5b7b133bb2ef839ae4fe86dec6783c89` | `fd1af4120ffb0a713330aab197c21e75c45ddbd177652e788e4f359d52c8a93d` |
| `docs/DATA_MODEL.md` | `7364fa9fc4ac67cd895611b57bece1b02fbb5ba81d49ac3649beaacad25d7be0` | `16fe17f0529a1c276d1dac3ead710e3f6147173b4570fa88361ec8e065eb6d5e` |
| `app/src/main/java/com/example/ungdungchiasecaycanh/data/remote/FirebaseAdminDataSource.java` | `c58f1ed398d1d1d12e77ba11a057f541e814626cf398de7ee61cb69cfe1ad720` | `4bd7af7b7fb00417dc82805559797e0c00b458c27875aa77baf840757418970c` |

## Fixture bổ sung có thể chạy lại

```javascript
// Local-only emulator fixtures. Existing firestore-tests files are intentionally unchanged.
import {createRequire} from 'node:module';
import {readFileSync} from 'node:fs';
import {before, after, beforeEach, test} from 'node:test';
import assert from 'node:assert/strict';
const require = createRequire(new URL('../../firestore-tests/package.json', import.meta.url));
const {initializeTestEnvironment, assertSucceeds, assertFails} = require('@firebase/rules-unit-testing');
const firebase = require('firebase/compat/app');
require('firebase/compat/firestore');
let env;
const now = () => firebase.firestore.FieldValue.serverTimestamp();
const db = uid => env.authenticatedContext(uid).firestore();
const profile = avatarUrl => ({displayName:'Fixture', locationText:'Q7', avatarUrl, bio:'', createdAt:now(), updatedAt:now()});
const media = (uid, kind='IMAGE') => ({ownerId:uid, conversationId:'alice__bob', kind, data:'x'.repeat(200), mime:kind==='IMAGE'?'image/jpeg':'audio/mp4', durationMs:kind==='IMAGE'?0:1000, createdAt:now()});
before(async () => {
  assert.match(process.env.FIRESTORE_EMULATOR_HOST || '', /^(127\.0\.0\.1|localhost):/);
  env = await initializeTestEnvironment({projectId:'demo-plantmate', firestore:{rules:readFileSync(new URL('../../firestore.rules', import.meta.url),'utf8')}});
});
after(async () => env?.cleanup());
beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async ctx => {
    const d=ctx.firestore();
    for(const uid of ['alice','bob','admin']) await d.doc(`users/${uid}`).set(profile(''));
    await d.doc('images/alicePhoto00001').set({ownerId:'alice',data:'x'.repeat(200),width:100,height:100,createdAt:now()});
    await d.doc('images/bobPhoto0000001').set({ownerId:'bob',data:'x'.repeat(200),width:100,height:100,createdAt:now()});
    await d.doc('conversations/alice__bob').set({listingId:'',ownerId:'alice',requesterId:'bob',participantIds:['alice','bob'],lastMessage:'history',createdAt:now(),updatedAt:now()});
    await d.doc('conversations/alice__bob/messages/history').set({senderId:'alice',type:'TEXT',text:'hello',createdAt:now()});
    await d.doc('chatMedia/existingPhoto01').set(media('alice'));
    await d.doc('admins/admin').set({});
    // Registration fixtures need users/alice to be absent when tested.
  });
});
for(const [label,avatar,allowed] of [
  ['empty','',true],['preset','preset:fern_1',true],['owned photo','pmimg://alicePhoto00001',true],
  ['other owner photo','pmimg://bobPhoto0000001',false],['missing photo','pmimg://missingPhoto001',false],
  ['http avatar','https://example.org/a.jpg',false],['local URI','content://local/photo',false],
  ['invalid preset','preset:UPPER',false],['invalid ref','pmimg://short',false],['non-string',17,false],
]) test(`profile create: ${label}`, async () => {
  await env.withSecurityRulesDisabled(ctx=>ctx.firestore().doc('users/alice').delete());
  await (allowed?assertSucceeds:assertFails)(db('alice').doc('users/alice').set(profile(avatar)));
});
test('profile update preserves legacy avatar but rejects a new foreign photo', async()=>{
  await env.withSecurityRulesDisabled(ctx=>ctx.firestore().doc('users/alice').update({avatarUrl:'https://legacy.example/a.jpg'}));
  await assertSucceeds(db('alice').doc('users/alice').update({bio:'updated',updatedAt:now()}));
  await assertFails(db('alice').doc('users/alice').update({avatarUrl:'pmimg://bobPhoto0000001',updatedAt:now()}));
});
for(const direction of ['none','alice','bob']) for(const kind of ['IMAGE','VOICE']) test(`chatMedia ${kind}, block=${direction}`, async()=>{
  if(direction!=='none') await env.withSecurityRulesDisabled(ctx=>ctx.firestore().doc(`users/${direction}/blockedUsers/${direction==='alice'?'bob':'alice'}`).set({createdAt:now()}));
  for(const uid of ['alice','bob']) {
    await (direction==='none'?assertSucceeds:assertFails)(db(uid).collection('chatMedia').add(media(uid,kind)));
    await assertSucceeds(db(uid).doc('chatMedia/existingPhoto01').get());
    await assertSucceeds(db(uid).doc('conversations/alice__bob/messages/history').get());
  }
  if(direction!=='none') {
    await env.withSecurityRulesDisabled(ctx=>ctx.firestore().doc(`users/${direction}/blockedUsers/${direction==='alice'?'bob':'alice'}`).delete());
    await assertSucceeds(db('alice').collection('chatMedia').add(media('alice',kind)));
  }
});
test('chatMedia outsider, signed-out and suspended writes denied; outsider cannot read',async()=>{
  await assertFails(db('stranger').collection('chatMedia').add(media('stranger')));
  await assertFails(env.unauthenticatedContext().firestore().collection('chatMedia').add(media('alice')));
  await assertFails(db('stranger').doc('chatMedia/existingPhoto01').get());
  await env.withSecurityRulesDisabled(ctx=>ctx.firestore().doc('suspendedUsers/alice').set({}));
  await assertFails(db('alice').collection('chatMedia').add(media('alice')));
});

// Emulator protocol fixtures for #43: exercise rules, batching, failure and retry.
// They do not execute the Android Java class; compile/unit checks cover it separately.
async function seedChildren(count, replies=false) {
  await env.withSecurityRulesDisabled(async ctx=>{
    const d=ctx.firestore();
    await d.doc('posts/P').set({authorId:'alice',text:'fixture',category:'GENERAL',listingId:'',imageUrl:'',createdAt:now()});
    if(replies) await d.doc('posts/P/comments/root').set({authorId:'bob',text:'root',replyToCommentId:'',createdAt:now()});
    for(let offset=0;offset<count;offset+=400){
      const b=d.batch();
      for(let i=offset;i<Math.min(count,offset+400);i++) {
        const path=replies||i%2===0 ? `posts/P/comments/c${i}` : `posts/P/likes/u${i}`;
        b.set(d.doc(path),path.includes('/comments/')?{authorId:'bob',text:'child',replyToCommentId:replies?'root':'',createdAt:now()}:{createdAt:now()});
      }
      await b.commit();
    }
  });
}
async function removeChildren(replies=false, afterChunk=async()=>{}, auditUid='admin') {
  const d=db('admin');
  const reads=replies ? [await d.collection('posts/P/comments').where('replyToCommentId','==','root').get({source:'server'})]
    : await Promise.all([d.collection('posts/P/comments').get({source:'server'}),d.collection('posts/P/likes').get({source:'server'})]);
  const refs=reads.flatMap(s=>s.docs.map(x=>x.ref));
  const sizes=[];
  for(let i=0;i<refs.length;i+=400){
    const b=d.batch(), chunk=refs.slice(i,i+400);
    chunk.forEach(ref=>b.delete(ref)); await b.commit(); sizes.push(chunk.length); await afterChunk(sizes.length);
  }
  const b=d.batch();
  b.delete(d.doc(replies?'posts/P/comments/root':'posts/P'));
  b.set(d.doc('adminActions/completed'),{adminId:auditUid,action:'DELETE',targetType:replies?'COMMENT':'POST',targetId:replies?'P/root':'P',note:'fixture',createdAt:now()});
  await b.commit(); sizes.push(2); return sizes;
}
test('admin deletes 810 mixed children in 400/400/10 batches then parent + audit',async()=>{
  await seedChildren(810);
  assert.deepEqual(await removeChildren(),[400,400,10,2]);
  const d=db('admin');
  assert.equal((await d.doc('posts/P').get()).exists,false);
  assert.equal((await d.collection('posts/P/comments').get()).size,0);
  assert.equal((await d.collection('posts/P/likes').get()).size,0);
  assert.equal((await d.doc('adminActions/completed').get()).exists,true);
});
test('admin deletes 805 replies before root; post remains',async()=>{
  await seedChildren(805,true);
  assert.deepEqual(await removeChildren(true),[400,400,5,2]);
  assert.equal((await db('admin').doc('posts/P').get()).exists,true);
  assert.equal((await db('admin').collection('posts/P/comments').get()).size,0);
});
test('permission loss after first chunk preserves parent/no audit; restored admin retries remaining',async()=>{
  await seedChildren(810);
  await assert.rejects(removeChildren(false,async n=>{if(n===1)await env.withSecurityRulesDisabled(ctx=>ctx.firestore().doc('admins/admin').delete());}));
  await env.withSecurityRulesDisabled(async ctx=>{
    const d=ctx.firestore();
    assert.equal((await d.doc('posts/P').get()).exists,true);
    assert.equal((await d.doc('adminActions/completed').get()).exists,false);
    assert.equal((await d.collection('posts/P/comments').get()).size+(await d.collection('posts/P/likes').get()).size,410);
    await d.doc('admins/admin').set({});
  });
  assert.deepEqual(await removeChildren(),[400,10,2]);
});
test('invalid final audit preserves parent atomically; retry succeeds with zero children',async()=>{
  await seedChildren(1);
  await assert.rejects(removeChildren(false,async()=>{},'alice'));
  assert.equal((await db('admin').doc('posts/P').get()).exists,true);
  assert.equal((await db('admin').doc('adminActions/completed').get()).exists,false);
  assert.deepEqual(await removeChildren(),[2]);
});
```
