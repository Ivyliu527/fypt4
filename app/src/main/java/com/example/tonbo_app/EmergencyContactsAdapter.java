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
            contactText.setContentDescription("緊急聯絡人：" + contact);
            
            removeButton.setOnClickListener(v -> {
                if (removeListener != null) {
                    removeListener.onContactRemove(contact);
                }
            });
            
            removeButton.setContentDescription("移除聯絡人：" + contact);
            
            // 設置觸控反饋
            VibrationManager vibrationManager = VibrationManager.getInstance(itemView.getContext());
            removeButton.setOnClickListener(v -> {
                vibrationManager.vibrateClick();
                if (removeListener != null) {
                    removeListener.onContactRemove(contact);
                }
            });
        }
    }
}
