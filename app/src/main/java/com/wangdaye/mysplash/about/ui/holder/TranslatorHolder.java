package com.wangdaye.mysplash.about.ui.holder;

import androidx.appcompat.widget.AppCompatImageView;
import android.view.View;
import android.widget.TextView;

import com.wangdaye.mysplash.R;
import com.wangdaye.mysplash.about.model.AboutModel;
import com.wangdaye.mysplash.common.basic.activity.MysplashActivity;
import com.wangdaye.mysplash.about.ui.AboutAdapter;
import com.wangdaye.mysplash.common.ui.widget.CircularImageView;
import com.wangdaye.mysplash.common.image.ImageHelper;
import com.wangdaye.mysplash.common.utils.helper.IntentHelper;
import com.wangdaye.mysplash.about.model.TranslatorObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Translator holder.
 *
 * This ViewHolder class is used to show translator for {@link AboutAdapter}.
 *
 * */

public class TranslatorHolder extends AboutAdapter.ViewHolder {

    @OnClick(R.id.item_about_translator_container) void clickItem() {
        String check = "^([a-z0-9A-Z]+[-|.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
        Pattern regex = Pattern.compile(check);
        Matcher matcher = regex.matcher(url);
        IntentHelper.startWebActivity(
                itemView.getContext(),
                matcher.matches() ? "mailto:" + url : url);
    }

    @BindView(R.id.item_about_translator_avatar) CircularImageView avatar;
    @BindView(R.id.item_about_translator_flag) AppCompatImageView flag;
    @BindView(R.id.item_about_translator_title) TextView title;
    @BindView(R.id.item_about_translator_subtitle) TextView subtitle;

    private String url;

    public TranslatorHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    @Override
    protected void onBindView(MysplashActivity a, AboutModel model) {
        TranslatorObject object = (TranslatorObject) model;

        ImageHelper.loadAvatar(a, avatar, object.avatarUrl, null);
        ImageHelper.loadResourceImage(a, flag, object.flagId);

        title.setText(object.title);
        subtitle.setText(object.subtitle);
        url = object.subtitle;
    }

    @Override
    protected void onRecycled() {
        ImageHelper.releaseImageView(avatar);
        ImageHelper.releaseImageView(flag);
    }
}
