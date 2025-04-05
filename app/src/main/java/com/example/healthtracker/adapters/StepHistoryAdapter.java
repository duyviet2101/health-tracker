package com.example.healthtracker.adapters;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthtracker.R;
import com.example.healthtracker.models.DailySummary;
import com.example.healthtracker.models.StepHistory;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class StepHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_DATE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;
    
    private Context context;
    private List<Object> items; // Danh sách các đối tượng để hiển thị (DailySummary hoặc StepHistory)
    
    public StepHistoryAdapter(Context context, List<Object> items) {
        this.context = context;
        this.items = items;
    }
    
    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof DailySummary) {
            return VIEW_TYPE_DATE_HEADER;
        } else {
            return VIEW_TYPE_ITEM;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        
        if (viewType == VIEW_TYPE_DATE_HEADER) {
            View view = inflater.inflate(R.layout.item_date_header, parent, false);
            return new DateHeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_step_history, parent, false);
            return new StepHistoryViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        
        if (holder instanceof DateHeaderViewHolder && item instanceof DailySummary) {
            DateHeaderViewHolder dateHolder = (DateHeaderViewHolder) holder;
            DailySummary summary = (DailySummary) item;
            dateHolder.bind(summary);
        } else if (holder instanceof StepHistoryViewHolder && item instanceof StepHistory) {
            StepHistoryViewHolder stepHolder = (StepHistoryViewHolder) holder;
            StepHistory history = (StepHistory) item;
            stepHolder.bind(history);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
    
    // ViewHolder cho mục tổng kết theo ngày
    class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        TextView tvTotalSteps;
        TextView tvTotalDistance;
        TextView tvTotalCalories;
        TextView tvTotalDuration;
        TextView tvGoalCompletion;
        ProgressBar progressGoal;
        
        public DateHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTotalSteps = itemView.findViewById(R.id.tvTotalSteps);
            tvTotalDistance = itemView.findViewById(R.id.tvTotalDistance);
            tvTotalCalories = itemView.findViewById(R.id.tvTotalCalories);
            tvTotalDuration = itemView.findViewById(R.id.tvTotalDuration);
            tvGoalCompletion = itemView.findViewById(R.id.tvGoalCompletion);
            progressGoal = itemView.findViewById(R.id.progressGoal);
        }
        
        public void bind(DailySummary summary) {
            // Định dạng ngày hiển thị
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String dateStr = dateFormat.format(summary.getDate());
            tvDate.setText(dateStr);
            
            // Hiển thị tổng số bước
            tvTotalSteps.setText(String.format("%,d bước", summary.getTotalSteps()).replace(",", "."));
            
            // Hiển thị tổng quãng đường
            tvTotalDistance.setText(String.format("%.2f km", summary.getTotalDistance()).replace(".", ","));
            
            // Hiển thị tổng calories
            tvTotalCalories.setText(String.format("%d kcal", summary.getTotalCalories()));
            
            // Hiển thị tổng thời gian
            String durationStr = formatDuration(summary.getTotalDuration());
            tvTotalDuration.setText(durationStr);
            
            // Hiển thị tiến độ hoàn thành mục tiêu
            int percentage = summary.getCompletionPercentage();
            String goalText;
            
            if (summary.getDailyGoalDistance() > 0) {
                // Hiển thị tiến độ với mục tiêu hiện tại - cải thiện định dạng
                goalText = String.format("Hoàn thành %.1f km / %.1f km (%d%%)",
                        summary.getTotalDistance(),
                        summary.getDailyGoalDistance(),
                        percentage)
                        .replace(".", ",");
            } else {
                goalText = "Chưa đặt mục tiêu hằng ngày";
            }
            
            tvGoalCompletion.setText(goalText);
            
            // Cập nhật progress bar
            progressGoal.setMax(100);
            progressGoal.setProgress(Math.min(percentage, 100));
            
            // Thay đổi màu chữ và progress bar dựa trên mức độ hoàn thành
            int colorId = getCompletionColorId(percentage);
            if (colorId != 0) {
                try {
                    int color = ContextCompat.getColor(context, colorId);
                    progressGoal.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
                    
                    // Nếu đạt hơn 100%, hiển thị màu xanh lá, nếu không giữ nguyên màu trắng
                    if (percentage >= 100) {
                        tvGoalCompletion.setTextColor(color);
                    }
                } catch (Exception e) {
                    // Sử dụng màu mặc định nếu có lỗi
                }
            }
        }
        
        // Lấy ID màu dựa trên phần trăm hoàn thành
        private int getCompletionColorId(int percentage) {
            if (percentage >= 100) {
                return android.R.color.holo_green_light; // Xanh lá khi hoàn thành
            } else if (percentage >= 75) {
                return android.R.color.holo_green_dark; // Xanh lá đậm khi gần hoàn thành
            } else if (percentage >= 50) {
                return android.R.color.holo_blue_light; // Xanh dương khi hoàn thành một nửa
            } else if (percentage >= 25) {
                return android.R.color.holo_orange_light; // Cam khi hoàn thành một phần
            } else {
                return android.R.color.holo_red_light; // Đỏ khi hoàn thành ít
            }
        }
    }
    
    // ViewHolder cho từng mục lịch sử
    class StepHistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime;
        TextView tvGoalStatus;
        TextView tvSteps;
        TextView tvDistance;
        TextView tvCalories;
        TextView tvDuration;
        ImageView ivActivityIcon;
        
        public StepHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvGoalStatus = itemView.findViewById(R.id.tvGoalStatus);
            tvSteps = itemView.findViewById(R.id.tvSteps);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvCalories = itemView.findViewById(R.id.tvCalories);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            ivActivityIcon = itemView.findViewById(R.id.ivActivityIcon);
        }
        
        public void bind(StepHistory history) {
            try {
                // Hiển thị thời gian rõ ràng hơn
                tvTime.setText("Thời gian: " + history.getTime());
                
                // Hiển thị trạng thái và mục tiêu
                tvGoalStatus.setVisibility(View.VISIBLE);
                float targetDistance = history.getTargetDistance();
                int completionPercentage = history.getCompletionPercentage();
                
                if (completionPercentage >= 100) {
                    tvGoalStatus.setText(String.format("Hoàn thành 100%% (Mục tiêu: %.2f km)", targetDistance).replace(".", ","));
                    tvGoalStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_light));
                } else {
                    tvGoalStatus.setText(String.format("Hoàn thành %d%% (Mục tiêu: %.2f km)", completionPercentage, targetDistance).replace(".", ","));
                    tvGoalStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_blue_light));
                }
                
                // Hiển thị các thông số chi tiết hơn
                tvSteps.setText(String.format("%,d", history.getSteps()).replace(",", "."));
                tvDistance.setText(String.format("%.2f km", history.getDistance()).replace(".", ","));
                tvCalories.setText(String.format("%d kcal", history.getCalories()));
                
                // Hiển thị thời gian rõ ràng hơn
                String durationStr = formatDuration(history.getDuration());
                tvDuration.setText(durationStr);
                
                // Đặt icon
                ivActivityIcon.setImageResource(R.drawable.ic_directions_walk_24dp);
                
                // Thêm Background màu khác để phân biệt với tổng kết
                itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
                
                // Log thông tin để debug
                Log.d("StepHistoryAdapter", "Hiển thị item: " + history.toString());
                
                // Bỏ sự kiện click vì không còn cần thiết
            } catch (Exception e) {
                Log.e("StepHistoryAdapter", "Lỗi bind dữ liệu: " + e.getMessage(), e);
            }
        }
    }
    
    // Định dạng thời gian từ milliseconds
    private String formatDuration(long durationInMillis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationInMillis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationInMillis) - 
                TimeUnit.MINUTES.toSeconds(minutes);
                
        if (minutes >= 60) {
            long hours = minutes / 60;
            minutes = minutes % 60;
            return String.format("%d giờ %d phút", hours, minutes);
        } else {
            return String.format("%d phút", minutes);
        }
    }
} 