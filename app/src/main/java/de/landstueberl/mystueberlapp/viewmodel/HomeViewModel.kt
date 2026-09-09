package de.landstueberl.mystueberlapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.landstueberl.mystueberlapp.data.Money
import de.landstueberl.mystueberlapp.data.Product
import de.landstueberl.mystueberlapp.data.db.entity.Image
import de.landstueberl.mystueberlapp.data.db.entity.ProductDetail
import de.landstueberl.mystueberlapp.repository.OrderRepository
import de.landstueberl.mystueberlapp.repository.ProductRepository
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Currency

class HomeViewModel(
    private val productRepo: ProductRepository,
    private val orderRepo: OrderRepository
): ViewModel() {

    fun createTestProduct() {
        viewModelScope.launch {
            val pDetail = ProductDetail(
                id = 0,
                description = "Test Product",
                purchasePrice = Money(
                    BigDecimal(20),
                    Currency.getInstance("EUR")
                ),
                salesPrice = Money(
                    BigDecimal(40),
                    Currency.getInstance("EUR")
                ),
                isSold = false,
                isRemoved = false
            )

            val images = listOf(
                Image(id = 0, productId = 0, filePath = "/test1.jpg")
            )

            val product = Product(
                details = pDetail,
                imageList = images
            )

            productRepo.upsertProduct(product)
        }
    }
}