package com.hamonie.activities

import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hamonie.appthemehelper.ThemeStore
import com.hamonie.appthemehelper.util.ATHUtil
import com.hamonie.appthemehelper.util.TintHelper
import com.hamonie.appthemehelper.util.ToolbarContentTintHelper
import com.hamonie.BuildConfig
import com.hamonie.R
import com.hamonie.activities.base.AbsBaseActivity
import com.hamonie.databinding.ActivityDonationBinding
import com.hamonie.extensions.textColorPrimary
import com.hamonie.extensions.textColorSecondary
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.PurchaseInfo
import com.anjlab.android.iab.v3.SkuDetails
import java.util.*

class SupportDevelopmentActivity : AbsBaseActivity(), BillingProcessor.IBillingHandler {

    lateinit var binding: ActivityDonationBinding

    companion object {
        val TAG: String = SupportDevelopmentActivity::class.java.simpleName
        const val DONATION_PRODUCT_IDS = R.array.donation_ids
    }

    var billingProcessor: BillingProcessor? = null

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    fun donate(i: Int) {
        val ids = resources.getStringArray(DONATION_PRODUCT_IDS)
        billingProcessor?.purchase(this, ids[i])
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDonationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setStatusbarColorAuto()
        setTaskDescriptionColorAuto()

        setupToolbar()

        billingProcessor = BillingProcessor(this, BuildConfig.GOOGLE_PLAY_LICENSING_KEY, this)
        TintHelper.setTint(binding.progress, ThemeStore.accentColor(this))
        binding.donation.setTextColor(ThemeStore.accentColor(this))
    }

    private fun setupToolbar() {
        val toolbarColor = ATHUtil.resolveColor(this, R.attr.colorSurface)
        binding.toolbar.setBackgroundColor(toolbarColor)
        ToolbarContentTintHelper.colorBackButton(binding.toolbar)
        setSupportActionBar(binding.toolbar)
    }

    override fun onBillingInitialized() {
        loadSkuDetails()
    }

    private fun loadSkuDetails() {
        binding.progressContainer.isVisible = true
        binding.recyclerView.isVisible = false
        val ids =
            resources.getStringArray(DONATION_PRODUCT_IDS)
        billingProcessor!!.getPurchaseListingDetailsAsync(
            ArrayList(listOf(*ids)),
            object : BillingProcessor.ISkuDetailsResponseListener {
                override fun onSkuDetailsResponse(skuDetails: MutableList<SkuDetails>?) {
                    if (skuDetails == null || skuDetails.isEmpty()) {
                        binding.progressContainer.isVisible = false
                        return
                    }

                    binding.progressContainer.isVisible = false
                    binding.recyclerView.apply {
                        itemAnimator = DefaultItemAnimator()
                        layoutManager = GridLayoutManager(this@SupportDevelopmentActivity, 2)
                        adapter = SkuDetailsAdapter(this@SupportDevelopmentActivity, skuDetails)
                        isVisible = true
                    }
                }

                override fun onSkuDetailsError(error: String?) {
                    Log.e(TAG, error.toString())
                }
            })
    }

    override fun onProductPurchased(productId: String, details: PurchaseInfo?) {
        // loadSkuDetails();
        Toast.makeText(this, R.string.thank_you, Toast.LENGTH_SHORT).show()
    }

    override fun onBillingError(errorCode: Int, error: Throwable?) {
        Log.e(TAG, "Billing error: code = $errorCode", error)
    }

    override fun onPurchaseHistoryRestored() {
        // loadSkuDetails();
        Toast.makeText(this, R.string.restored_previous_purchases, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        billingProcessor?.release()
        super.onDestroy()
    }
}

class SkuDetailsAdapter(
    private var donationsDialog: SupportDevelopmentActivity,
    objects: List<SkuDetails>
) : RecyclerView.Adapter<SkuDetailsAdapter.ViewHolder>() {

    private var skuDetailsList: List<SkuDetails> = ArrayList()

    init {
        skuDetailsList = objects
    }

    private fun getIcon(position: Int): Int {
        return when (position) {
            0 -> R.drawable.ic_cookie
            1 -> R.drawable.ic_take_away
            2 -> R.drawable.ic_take_away_coffe
            3 -> R.drawable.ic_beer
            4 -> R.drawable.ic_fast_food_meal
            5 -> R.drawable.ic_popcorn
            6 -> R.drawable.ic_card_giftcard
            7 -> R.drawable.ic_food_croissant
            else -> R.drawable.ic_card_giftcard
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(donationsDialog).inflate(
                LAYOUT_RES_ID,
                viewGroup,
                false
            )
        )
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        val skuDetails = skuDetailsList[i]
        viewHolder.title.text = skuDetails.title.replace("Music Player - MP3 Player - Retro", "")
            .trim { it <= ' ' }
        viewHolder.text.text = skuDetails.description
        viewHolder.text.isVisible = false
        viewHolder.price.text = skuDetails.priceText
        viewHolder.image.setImageResource(getIcon(i))

        val purchased = donationsDialog.billingProcessor!!.isPurchased(skuDetails.productId)
        val titleTextColor = if (purchased) ATHUtil.resolveColor(
            donationsDialog,
            android.R.attr.textColorHint
        ) else donationsDialog.textColorPrimary()
        val contentTextColor =
            if (purchased) titleTextColor else donationsDialog.textColorSecondary()

        viewHolder.title.setTextColor(titleTextColor)
        viewHolder.text.setTextColor(contentTextColor)
        viewHolder.price.setTextColor(titleTextColor)

        strikeThrough(viewHolder.title, purchased)
        strikeThrough(viewHolder.text, purchased)
        strikeThrough(viewHolder.price, purchased)

        viewHolder.itemView.setOnTouchListener { _, _ -> purchased }
        viewHolder.itemView.setOnClickListener { donationsDialog.donate(i) }
    }

    override fun getItemCount(): Int {
        return skuDetailsList.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var title: TextView = view.findViewById(R.id.itemTitle)
        var text: TextView = view.findViewById(R.id.itemText)
        var price: TextView = view.findViewById(R.id.itemPrice)
        var image: AppCompatImageView = view.findViewById(R.id.itemImage)
    }

    companion object {
        @LayoutRes
        private val LAYOUT_RES_ID = R.layout.item_donation_option

        private fun strikeThrough(textView: TextView, strikeThrough: Boolean) {
            textView.paintFlags =
                if (strikeThrough) textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                else textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }
}
