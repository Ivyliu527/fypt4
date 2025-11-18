package com.example.tonbo_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * 緊急聯絡人列表適配器
 */
public class EmergencyContactsAdapter extends RecyclerView.Adapter<EmergencyContactsAdapter.ContactViewHolder> {
    
    private List<String> contacts;
    private OnContactRemoveListener removeListener;
    
    public interface OnContactRemoveListener {
        void onContactRemove(String contact);
    }
    
    public EmergencyContactsAdapter(List<String> contacts, OnContactRemoveListener removeListener) {
        this.contacts = contacts;
        this.removeListener = removeListener;
    }
    
    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_emergency_contact, parent, false);
        return new ContactViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        String contact = contacts.get(position);
        holder.bind(contact);
    }
    
    @Override
    public int getItemCount() {
        return contacts.size();
    }
    
    class ContactViewHolder extends RecyclerView.ViewHolder {
        private TextView contactText;
        private Button removeButton;
        
        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            contactText = itemView.findViewById(R.id.contactText);
            removeButton = itemView.findViewById(R.id.removeButton);
        }
        
        public void bind(String contact) {
            contactText.setText(contact);
            
            // 將電話號碼轉換為逐個數字讀出的格式（用於無障礙播報）
            String digitByDigitContact = formatPhoneNumberForAccessibility(contact);
            contactText.setContentDescription("緊急聯絡人：" + digitByDigitContact);
            
            removeButton.setOnClickListener(v -> {
                if (removeListener != null) {
                    removeListener.onContactRemove(contact);
                }
            });
            
            // 移除按鈕的內容描述也使用逐個數字格式
            removeButton.setContentDescription("移除聯絡人：" + digitByDigitContact);
            
            // 設置觸控反饋
            VibrationManager vibrationManager = VibrationManager.getInstance(itemView.getContext());
            removeButton.setOnClickListener(v -> {
                vibrationManager.vibrateClick();
                if (removeListener != null) {
                    removeListener.onContactRemove(contact);
                }
            });
        }
        
        /**
         * 將電話號碼格式化為逐個數字讀出的格式
         * 例如："999" -> "九 九 九" 或 "nine nine nine"
         *      "+852-1234-5678" -> "加號 八 五 二 一 二 三 四 五 六 七 八"
         */
        private String formatPhoneNumberForAccessibility(String phoneNumber) {
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                return phoneNumber;
            }
            
            StringBuilder result = new StringBuilder();
            String currentLang = LocaleManager.getInstance(itemView.getContext()).getCurrentLanguage();
            
            for (char c : phoneNumber.toCharArray()) {
                if (Character.isDigit(c)) {
                    // 數字：轉換為逐個數字讀出
                    if (result.length() > 0) {
                        result.append(" "); // 數字之間加空格
                    }
                    
                    if ("english".equals(currentLang)) {
                        // 英文：直接讀數字
                        result.append(c);
                    } else {
                        // 中文：轉換為中文數字
                        switch (c) {
                            case '0': result.append("零"); break;
                            case '1': result.append("一"); break;
                            case '2': result.append("二"); break;
                            case '3': result.append("三"); break;
                            case '4': result.append("四"); break;
                            case '5': result.append("五"); break;
                            case '6': result.append("六"); break;
                            case '7': result.append("七"); break;
                            case '8': result.append("八"); break;
                            case '9': result.append("九"); break;
                            default: result.append(c); break;
                        }
                    }
                } else if (c == '+') {
                    // 加號
                    if (result.length() > 0) {
                        result.append(" ");
                    }
                    if ("english".equals(currentLang)) {
                        result.append("plus");
                    } else {
                        result.append("加號");
                    }
                } else if (c == '-') {
                    // 連字符：用"橫線"表示
                    if (result.length() > 0) {
                        result.append(" ");
                    }
                    if ("english".equals(currentLang)) {
                        result.append("dash");
                    } else {
                        result.append("橫線");
                    }
                } else if (c == '(' || c == ')') {
                    // 括號：跳過或讀出
                    if (result.length() > 0) {
                        result.append(" ");
                    }
                    if ("english".equals(currentLang)) {
                        result.append(c == '(' ? "left parenthesis" : "right parenthesis");
                    } else {
                        result.append(c == '(' ? "左括號" : "右括號");
                    }
                } else if (Character.isWhitespace(c)) {
                    // 空格：保留
                    result.append(" ");
                } else {
                    // 其他字符：直接保留
                    if (result.length() > 0) {
                        result.append(" ");
                    }
                    result.append(c);
                }
            }
            
            return result.toString().trim();
        }
    }
}
