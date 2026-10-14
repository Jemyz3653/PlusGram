/*
 * PlusGram for Android.
 * Edit history dialog (AyuGram feature).
 */

package com.plusgram.messenger.ui;

import static org.telegram.messenger.LocaleController.getString;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;

import com.plusgram.messenger.ayu.AyuMessages;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;

public final class PlusEditHistory {

    private PlusEditHistory() {
    }

    public static void show(BaseFragment fragment, MessageObject messageObject) {
        if (fragment == null || messageObject == null || messageObject.messageOwner == null) {
            return;
        }
        final int account = fragment.getCurrentAccount();
        AyuMessages.getInstance().loadEditHistory(account, messageObject.getDialogId(), messageObject.getId(), versions -> {
            if (fragment.getParentActivity() == null) {
                return;
            }
            SpannableStringBuilder text = new SpannableStringBuilder();
            if (versions.isEmpty()) {
                text.append(getString(R.string.AyuEditHistoryEmpty));
            } else {
                for (int i = 0; i < versions.size(); i++) {
                    AyuMessages.EditedVersion version = versions.get(i);
                    appendVersion(text, LocaleController.formatDateTime(version.editDate, true), version.text);
                }
                String current = messageObject.messageOwner.message;
                appendVersion(text, getString(R.string.AyuEditHistoryCurrent), current);
            }
            final String plain = text.toString();
            AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
            builder.setTitle(getString(R.string.AyuEditHistory));
            builder.setMessage(text);
            builder.setPositiveButton(getString(R.string.OK), null);
            if (!versions.isEmpty()) {
                builder.setNeutralButton(getString(R.string.Copy), (dialog, which) -> {
                    AndroidUtilities.addToClipboard(plain);
                    BulletinFactory.of(fragment).createCopyBulletin(getString(R.string.PlusCopied)).show();
                });
            }
            fragment.showDialog(builder.create());
        });
    }

    private static void appendVersion(SpannableStringBuilder builder, String title, String body) {
        if (builder.length() > 0) {
            builder.append("\n\n");
        }
        int start = builder.length();
        builder.append(title);
        builder.setSpan(new StyleSpan(Typeface.BOLD), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append("\n");
        builder.append(TextUtils.isEmpty(body) ? "—" : body);
    }
}
