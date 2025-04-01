# Git Workflow

## Quy trình làm việc

1. **Kiểm tra nhánh chính (main)**:
    - Mỗi khi bắt đầu làm việc trên một tính năng mới hoặc sửa lỗi, bạn cần chuyển sang nhánh `main`
      và cập nhật mã nguồn mới nhất:
      ```bash
      git checkout main
      git pull origin main
      ```

2. **Tạo nhánh mới**:
    - Sau khi cập nhật mã nguồn từ nhánh `main`, tạo một nhánh mới để làm việc trên tính năng hoặc
      sửa lỗi.
    - Đặt tên nhánh theo định dạng sau:
      `<tên người code>/<feature hoặc fix>/<tên tính năng hoặc lỗi>`
        - Ví dụ: `hoang/feature/user-login` hoặc `linh/fix/crash-on-startup`
      ```bash
      git checkout -b <tên người code>/<feature hoặc fix>/<tên tính năng hoặc lỗi>
      ```

3. **Phát triển tính năng hoặc sửa lỗi**:
    - Tiến hành code tính năng mới hoặc sửa lỗi trong nhánh mới đã tạo.

4. **Commit và Push**:
    - Sau khi hoàn thành phần code, commit và push lên nhánh đã tạo.
    - Đảm bảo commit có thông điệp rõ ràng, mô tả chính xác các thay đổi.
      ```bash
      git add .
      git commit -m "Mô tả thay đổi của bạn"
      git push origin <tên nhánh>
      ```

5. **Tạo Pull Request (PR)**:
    - Sau khi đã push mã nguồn lên, tạo một Pull Request (PR) từ nhánh của bạn vào nhánh `main`.

6. **Kiểm tra tự động (Git Actions)**:
    - Một GitAction sẽ tự động kiểm tra lint và build code để đảm bảo không có lỗi.
    - Chờ kết quả từ GitAction. Nếu có lỗi, bạn cần sửa lại và push lại mã nguồn.

7. **Sửa lỗi và Merge**:
    - Nếu có lỗi từ GitAction, sửa lại và push mã nguồn một lần nữa.
    - Sau khi sửa xong và tất cả các kiểm tra thành công, yêu cầu thành viên khác xem lại và merge
      PR vào nhánh `main`.

## Lưu ý

- Luôn đảm bảo rằng mã nguồn luôn sạch và không có lỗi trước khi merge vào nhánh chính.
- Cập nhật và đồng bộ mã nguồn từ nhánh chính thường xuyên để tránh xung đột.