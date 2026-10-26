# Hướng dẫn đóng góp TreeShare / PlantMate

Nhánh chính là `main`; thay `<tep-thuoc-pham-vi>` bằng file thực. Ví dụ dùng issue GitHub #28. Repo hiện có prototype; công việc Android cần bộ mã nền đã được trưởng nhóm bàn giao.

1. Nhận issue được giao và kiểm tra phạm vi file trong ISSUES_PLAN.md.
2. Clone nếu chưa có; chuyển về nhánh `main` và cập nhật trước khi làm.
3. Tạo branch riêng theo issue, ví dụ `issue-28-update-readme`.
4. Chỉ sửa file theo checklist issue; với Android, đọc AGENTS.md và tài liệu trong bộ mã nền đã được bàn giao.
5. Kiểm tra thay đổi, chỉ thêm đúng file và commit với thông điệp rõ ràng.
6. Push branch riêng lên repository.
7. Tạo Pull Request bằng giao diện GitHub; mô tả thay đổi, kết quả kiểm tra và `Closes #<issue-number>` bằng số thật.
8. Trưởng nhóm **nguyencaodien** review và merge Pull Request; sửa phản hồi trên cùng branch.

```powershell
git clone https://github.com/nguyencaodien/TreeShare.git
Set-Location TreeShare
git switch main
git pull --ff-only
git switch -c issue-28-update-readme
git status
git add <tep-thuoc-pham-vi>
git commit -m "docs: cap nhat README (#28)"
git push -u origin issue-28-update-readme
```
