/*
 * PlusGram for Android.
 * List of saved deleted messages (AyuGram feature).
 */

package com.plusgram.messenger.ui;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.plusgram.messenger.ayu.AyuMessages;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

public class AyuDeletedMessagesActivity extends UniversalFragment {

    private static final int LIMIT = 300;

    private final ArrayList<AyuMessages.DeletedMessage> messages = new ArrayList<>();
    private final ArrayList<View> views = new ArrayList<>();
    private boolean loading = true;

    @Override
    public boolean onFragmentCreate() {
        AyuMessages.getInstance().loadDeleted(currentAccount, LIMIT, result -> {
            loading = false;
            messages.clear();
            messages.addAll(result);
            views.clear();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(false);
            }
        });
        return super.onFragmentCreate();
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.AyuDeletedList);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (loading) {
            items.add(UItem.asFlicker(org.telegram.ui.Components.FlickerLoadingView.DIALOG_CELL_TYPE));
            return;
        }
        if (messages.isEmpty()) {
            items.add(UItem.asShadow(getString(R.string.AyuDeletedEmpty)));
            return;
        }
        if (views.size() != messages.size() && getContext() != null) {
            views.clear();
            for (int i = 0; i < messages.size(); i++) {
                views.add(createMessageView(getContext(), messages.get(i), i));
            }
        }
        for (int i = 0; i < views.size(); i++) {
            UItem item = UItem.asCustom(views.get(i));
            item.id = 1000 + i;
            items.add(item);
        }
        items.add(UItem.asShadow(null));
    }

    private String getPeerName(long id) {
        if (id > 0) {
            TLRPC.User user = getMessagesController().getUser(id);
            if (user != null) {
                return UserObject.getUserName(user);
            }
        } else if (id < 0) {
            TLRPC.Chat chat = getMessagesController().getChat(-id);
            if (chat != null) {
                return chat.title;
            }
        }
        return "ID " + id;
    }

    private View createMessageView(Context context, AyuMessages.DeletedMessage message, int index) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(21), dp(10), dp(21), dp(10));
        layout.setBackground(Theme.getSelectorDrawable(false));

        String chatName = getPeerName(message.dialogId);
        String senderName = message.fromId != message.dialogId ? getPeerName(message.fromId) : null;
        String header = TextUtils.isEmpty(senderName) ? chatName : chatName + " · " + senderName;

        TextView title = new TextView(context);
        title.setText(header);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        layout.addView(title);

        TextView text = new TextView(context);
        text.setText(message.text);
        text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        text.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        text.setPadding(0, dp(3), 0, dp(3));
        layout.addView(text);

        TextView date = new TextView(context);
        date.setText(LocaleController.formatDateTime(message.date, true) + " · " + formatString(R.string.AyuDeletedAt, LocaleController.formatDateTime(message.deletedDate, true)));
        date.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        date.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        layout.addView(date);

        layout.setOnClickListener(v -> presentFragment(ChatActivity.of(message.dialogId, message.messageId)));
        layout.setOnLongClickListener(v -> {
            AndroidUtilities.addToClipboard(message.text);
            BulletinFactory.of(this).createCopyBulletin(getString(R.string.PlusCopied)).show();
            return true;
        });
        return layout;
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
