package jp.wildtree.android.apps.hhsadvrev

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.Button
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import android.widget.TextView
import androidx.transition.Visibility


class FontSizeInlinePreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.preferenceStyle
) : Preference(context, attrs, defStyleAttr) {
    private var selectedSize: Int = 16
    init {
    // 標準のタイトル・サマリーは残し、右側のウィジェット領域だけ差し替える
        layoutResource = R.layout.custom_fontsize_preference
        //widgetLayoutResource = R.layout.font_size_widget
    }

    // テーマ属性から色を取得するヘルパー関数
    @ColorInt
    private fun getThemeColor(context: Context, @AttrRes attrResId: Int): Int {
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(attrResId, typedValue, true)
        return typedValue.data
    }
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        // タイトル用の TextView を取得して設定
        val titleView = holder.itemView.findViewById<TextView>(android.R.id.title)
        titleView?.text = title // Preference の title プロパティからタイトルを取得

        val iconFrame = holder.findViewById(android.R.id.icon_frame) as? LinearLayout
        //iconFrame?.visibility = holder.itemView.visibility;

        val container = holder.itemView.findViewById<LinearLayout>(R.id.fontSizeContainer)
        container?.let {
            it.removeAllViews()
            val sizes = listOf(12, 16, 20, 24)
            // テーマから色を取得
            // 例: プライマリカラー (選択状態のボタン用)
            val selectedButtonColor = getThemeColor(context, R.attr.colorPrimary) // Material Components を使っている場合
            // val selectedButtonColor = getThemeColor(context, R.attr.colorPrimary) // あなたのアプリのテーマで定義されたcolorPrimary

            // 例: 通常のボタンの背景色 (非選択状態のボタン用) - 例えば colorControlNormal や colorSurface
            // ここでは仮に固定色か、別のテーマ属性を使います。
            // Material Components のテーマで、ボタンのデフォルト背景色に近いものを取得するには、
            // 少し工夫が必要な場合があります。多くの場合、デフォルトのボタンスタイルがそれらを処理します。
            // ここでは例として、選択されていないボタンは少し薄い色にするか、
            // もしくはテーマの colorSurface や、明示的なグレーなどを使います。
            val defaultButtonBackgroundColor = getThemeColor(context, R.attr.colorSurfaceVariant) // 例: Material 3 の Surface Variant
            // val defaultButtonNonSelectedColor = Color.parseColor("#E0E0E0") // もしくは固定色

            // または、選択されていないボタンのテキスト色をアクセントにするなども考えられます。
            val defaultButtonTextColor = getThemeColor(context, R.attr.colorOnSurface)
            sizes.forEach { size ->
                val button = Button(context).apply {
                    text = "Aあ"
                    textSize = size.toFloat()
                    minWidth = 0
                    setPadding(2, 8, 2, 8)

                    if (size == selectedSize) {
                        // 選択されているボタン
                        backgroundTintList = ColorStateList.valueOf(selectedButtonColor)
                        // 選択されているボタンのテキスト色 (プライマリカラーの上なので、通常は白系統)
                        setTextColor(
                            getThemeColor(
                                context,
                                R.attr.colorOnPrimary
                            )
                        )
                    } else {
                        // 選択されていないボタン
                        // 背景色を colorSurfaceVariant (または他の適切な色) にする
                        backgroundTintList = ColorStateList.valueOf(defaultButtonBackgroundColor)
                        // テキスト色を colorOnSurface (または他の適切な色) にする
                        setTextColor(defaultButtonTextColor)

                        // もし、選択されていないボタンはデフォルトのボタンスタイル（テーマに準拠）のままにしたい場合は、
                        // backgroundTintList や setTextColor をここでは設定しないという選択もあります。
                    }
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(0, 0, 0, 0)
                    layoutParams = params

                    setOnClickListener {
                        selectedSize = size
                        persistInt(size)
                        notifyChanged()
                    }
                }
                it.addView(button)
            }
        }
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        super.onSetInitialValue(defaultValue)
        selectedSize = getPersistedInt((defaultValue as? String)?.toIntOrNull() ?: 16)
    }
}
