package com.kadaiku.brunei

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.UUID

data class UserAccount(val id:String=UUID.randomUUID().toString(),val username:String,val password:String,val role:String,val shop:String="",val phone:String="",val address:String="")
data class SellerProfile(val shop:String,val phone:String,val address:String)
data class Product(val id:String=UUID.randomUUID().toString(),val name:String,val price:Double,val shop:String,val category:String,val area:String,val image:String,val sellerPhone:String,val sellerAddress:String,val sellerId:String="sample")
data class CartLine(val product:Product,var qty:Int)
data class Order(
    val id:String=UUID.randomUUID().toString().take(8).uppercase(),
    val buyerId:String="",
    val lines:List<CartLine>,
    val delivery:String,
    val district:String="",
    val deliveryFee:Double=0.0,
    val paymentMethod:String="Cash",
    val total:Double,
    val runnerName:String="",
    val runnerPhone:String="",
    val createdAt:Long=System.currentTimeMillis(),
    val deliveredAt:Long?=null
)

@Composable fun money(v:Double)=Text("B$%.2f".format(v))
class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{KadaiKuApp()}}}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun KadaiKuApp(){
    var accounts by remember{mutableStateOf(listOf<UserAccount>())}
    var currentUser by remember{mutableStateOf<UserAccount?>(null)}
    var showRegister by remember{mutableStateOf(false)}
    var tab by remember{mutableIntStateOf(0)}
    var products by remember{mutableStateOf(listOf(
        Product(name="Kek Coklat Premium",price=15.0,shop="Dapur Aisyah",category="Makanan",area="Gadong",image="🍰",sellerPhone="+673 7XX XXXX",sellerAddress="Gadong, Brunei-Muara",sellerId="sample-aisyah"),
        Product(name="Perfume Oud",price=20.0,shop="Brunei Fragrance",category="Beauty",area="Kiulap",image="🧴",sellerPhone="+673 8XX XXXX",sellerAddress="Kiulap, Brunei-Muara",sellerId="sample-fragrance"),
        Product(name="Baju Melayu",price=35.0,shop="Kedai Kita",category="Fashion",area="Bandar",image="👕",sellerPhone="+673 7XX XXXX",sellerAddress="Bandar Seri Begawan",sellerId="sample-kita"),
        Product(name="Frozen Food Combo",price=12.0,shop="Mama's Frozen",category="Makanan",area="Tutong",image="🥟",sellerPhone="+673 8XX XXXX",sellerAddress="Tutong, Brunei",sellerId="sample-mama")
    ))}
    var cart by remember{mutableStateOf(listOf<CartLine>())}
    var orders by remember{mutableStateOf(listOf<Order>())}
    var search by remember{mutableStateOf("")}
    var showSeller by remember{mutableStateOf(false)}
    var showAdd by remember{mutableStateOf(false)}
    var selectedProduct by remember{mutableStateOf<Product?>(null)}

    if(currentUser==null){
        if(showRegister){
            RegisterScreen(
                onBack={showRegister=false},
                onRegister={username,password,role,shop,phone,address->
                    if(accounts.none{it.username.equals(username,true)}){
                        val account=UserAccount(username=username.trim(),password=password,role=role,shop=shop.trim(),phone=phone.trim(),address=address.trim())
                        accounts=accounts+account
                        currentUser=account
                        showRegister=false
                    }
                }
            )
        }else{
            LoginScreen(
                onLogin={username,password->accounts.firstOrNull{it.username.equals(username.trim(),true)&&it.password==password}?.let{currentUser=it}},
                onRegister={showRegister=true}
            )
        }
        return
    }

    val user=currentUser!!
    val sellerProfile=SellerProfile(user.shop.ifBlank{"Kedai Saya"},user.phone,user.address)
    val filtered=products.filter{search.isBlank()||it.name.contains(search,true)||it.shop.contains(search,true)||it.category.contains(search,true)}
    val buyerOrders=orders.filter{it.buyerId==user.id}

    Scaffold(
        containerColor=Color.White,
        topBar={CenterAlignedTopAppBar(title={Text("KadaiKu 🇧🇳",fontWeight=FontWeight.Bold,color=Color(0xFF0D47A1))},colors=TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor=Color.White))},
        bottomBar={NavigationBar(containerColor=Color.White){listOf("Home","Search","Cart (${cart.sumOf{it.qty}})","Profile").forEachIndexed{i,l->NavigationBarItem(selected=tab==i,onClick={tab=i},icon={},label={Text(l)},colors=NavigationBarItemDefaults.colors(selectedTextColor=Color(0xFF0D47A1),indicatorColor=Color(0xFFE3F2FD)))}}}
    ){pad->
        when(tab){
            0->Home(filtered,search,{search=it},{if(user.role=="Seller")showSeller=true},{p->cart=addCart(cart,p)},{selectedProduct=it},Modifier.padding(pad),user.role=="Seller")
            1->SearchPage(filtered,search,{search=it},{p->cart=addCart(cart,p)},{selectedProduct=it},Modifier.padding(pad))
            2->CartPage(
                c=cart,
                minus={p->cart=removeOne(cart,p)},
                plus={p->cart=addCart(cart,p)},
                remove={p->cart=removeItem(cart,p)},
                checkout={delivery,district,fee,payment,total->
                    if(cart.isNotEmpty()){
                        val runner=if(delivery=="Pickup") Pair("","") else assignMarketplaceRunner()
                        orders=orders+Order(buyerId=user.id,lines=cart.map{it.copy()},delivery=delivery,district=district,deliveryFee=fee,paymentMethod=payment,total=total,runnerName=runner.first,runnerPhone=runner.second)
                        cart=listOf();tab=3
                    }
                },
                m=Modifier.padding(pad)
            )
            else->ProfilePage(
                orders=buyerOrders,
                user=user,
                seller={if(user.role=="Seller")showSeller=true},
                logout={currentUser=null;cart=listOf();tab=0},
                markDelivered={orderId->orders=orders.map{o->if(o.id==orderId&&o.deliveredAt==null)o.copy(deliveredAt=System.currentTimeMillis())else o}},
                m=Modifier.padding(pad)
            )
        }
    }
    selectedProduct?.let{p->ProductDetailSheet(product=p,onClose={selectedProduct=null},onAdd={cart=addCart(cart,p)})}
    if(showSeller&&user.role=="Seller") SellerSheet(onClose={showSeller=false},onAdd={showAdd=true},products=products,orders=orders,profile=sellerProfile,sellerId=user.id,onDelete={id->products=products.filterNot{it.id==id}})
    if(showAdd&&user.role=="Seller") AddProductDialog(sellerProfile=sellerProfile,onDismiss={showAdd=false},onSave={name,price,cat,area,image->products=products+Product(name=name,price=price,shop=sellerProfile.shop.ifBlank{"Kedai Saya"},category=cat,area=area,image=image,sellerPhone=sellerProfile.phone,sellerAddress=sellerProfile.address,sellerId=user.id);showAdd=false})
}

@Composable fun LoginScreen(onLogin:(String,String)->Unit,onRegister:()->Unit){var u by remember{mutableStateOf("")};var p by remember{mutableStateOf("")};Column(Modifier.fillMaxSize().background(Color.White).padding(24.dp),verticalArrangement=Arrangement.Center){Text("KadaiKu Brunei",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.ExtraBold,color=Color(0xFF0D47A1));Text("Login ke akaun kita",color=Color.Gray);Spacer(Modifier.height(20.dp));OutlinedTextField(u,{u=it},label={Text("Username")},modifier=Modifier.fillMaxWidth());Spacer(Modifier.height(10.dp));OutlinedTextField(p,{p=it},label={Text("Password")},modifier=Modifier.fillMaxWidth());Spacer(Modifier.height(14.dp));Button(onClick={onLogin(u,p)},modifier=Modifier.fillMaxWidth(),enabled=u.isNotBlank()&&p.isNotBlank(),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF1565C0))){Text("Login")};TextButton(onClick=onRegister,modifier=Modifier.fillMaxWidth()){Text("New user? Register")}}}

@Composable fun RegisterScreen(onBack:()->Unit,onRegister:(String,String,String,String,String,String)->Unit){var u by remember{mutableStateOf("")};var p by remember{mutableStateOf("")};var role by remember{mutableStateOf("Buyer")};var shop by remember{mutableStateOf("")};var phone by remember{mutableStateOf("")};var address by remember{mutableStateOf("")};LazyColumn(Modifier.fillMaxSize().background(Color.White),contentPadding=PaddingValues(24.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text("Create Account",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.ExtraBold,color=Color(0xFF0D47A1))};item{OutlinedTextField(u,{u=it},label={Text("Username")},modifier=Modifier.fillMaxWidth())};item{OutlinedTextField(p,{p=it},label={Text("Password")},modifier=Modifier.fillMaxWidth())};item{Text("Account type",fontWeight=FontWeight.Bold);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=role=="Buyer",onClick={role="Buyer"},label={Text("Buyer")});FilterChip(selected=role=="Seller",onClick={role="Seller"},label={Text("Seller")})}};if(role=="Seller"){item{OutlinedTextField(shop,{shop=it},label={Text("Nama kedai")},modifier=Modifier.fillMaxWidth())};item{OutlinedTextField(phone,{phone=it},label={Text("No. telefon seller")},modifier=Modifier.fillMaxWidth())};item{OutlinedTextField(address,{address=it},label={Text("Alamat kedai")},modifier=Modifier.fillMaxWidth())}};item{Button(onClick={onRegister(u,p,role,shop,phone,address)},modifier=Modifier.fillMaxWidth(),enabled=u.isNotBlank()&&p.length>=4&&(role=="Buyer"||(shop.isNotBlank()&&phone.isNotBlank()&&address.isNotBlank())),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF1565C0))){Text("Create Account")}};item{TextButton(onClick=onBack,modifier=Modifier.fillMaxWidth()){Text("Back to Login")}};item{Text("Demo local account: data/password belum sync cloud. Untuk production perlu backend authentication.",style=MaterialTheme.typography.bodySmall,color=Color.Gray)}}}

fun assignMarketplaceRunner():Pair<String,String>{val runners=listOf(Pair("Hakim","+673 7XX 1010"),Pair("Faris","+673 8XX 2020"),Pair("Azim","+673 7XX 3030"),Pair("Rizal","+673 8XX 4040"));return runners.random()}
fun deliveryFeeFor(district:String):Double=when(district){"Brunei-Muara"->3.0;"Tutong"->5.0;"Kuala Belait"->6.0;"Temburong"->8.0;else->3.0}
fun addCart(c:List<CartLine>,p:Product):List<CartLine>{val x=c.toMutableList();val i=x.indexOfFirst{it.product.id==p.id};if(i>=0)x[i]=x[i].copy(qty=x[i].qty+1)else x.add(CartLine(p,1));return x}
fun removeOne(c:List<CartLine>,p:Product):List<CartLine>{val x=c.toMutableList();val i=x.indexOfFirst{it.product.id==p.id};if(i>=0){if(x[i].qty>1)x[i]=x[i].copy(qty=x[i].qty-1)else x.removeAt(i)};return x}
fun removeItem(c:List<CartLine>,p:Product):List<CartLine> = c.filterNot{it.product.id==p.id}
fun sellerOrderValue(order:Order,sellerId:String):Double=order.lines.filter{it.product.sellerId==sellerId}.sumOf{it.product.price*it.qty}

@Composable fun ProductImage(image:String,modifier:Modifier=Modifier){if(image.startsWith("content://")||image.startsWith("file://")){Image(painter=rememberAsyncImagePainter(Uri.parse(image)),contentDescription="Product image",modifier=modifier,contentScale=ContentScale.Crop)}else{Box(modifier=modifier.background(Color(0xFFF5F9FF)),contentAlignment=Alignment.Center){Text(image.ifBlank{"🛍️"},style=MaterialTheme.typography.headlineLarge)}}}

@Composable fun Home(ps:List<Product>,search:String,setSearch:(String)->Unit,seller:()->Unit,add:(Product)->Unit,view:(Product)->Unit,m:Modifier,isSeller:Boolean){LazyColumn(modifier=m.fillMaxSize().background(Color.White),contentPadding=PaddingValues(bottom=20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){item{Box(modifier=Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF0D47A1),Color(0xFF1976D2),Color(0xFF42A5F5)))).padding(horizontal=20.dp,vertical=18.dp)){Column{Row(verticalAlignment=Alignment.CenterVertically){Box(modifier=Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha=0.18f)),contentAlignment=Alignment.Center){Box(modifier=Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White),contentAlignment=Alignment.Center){Text("K",fontWeight=FontWeight.Black,color=Color(0xFF1565C0),style=MaterialTheme.typography.headlineMedium)}};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text("KadaiKu Brunei",color=Color.White,fontWeight=FontWeight.ExtraBold,style=MaterialTheme.typography.headlineSmall);Text("Marketplace local kitani 🇧🇳",color=Color.White.copy(alpha=0.92f))}};Spacer(Modifier.height(14.dp));Text("Cari barang yang kita mau",color=Color.White,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(8.dp));SearchBox(search,setSearch)}}};item{Column(Modifier.padding(horizontal=16.dp)){Text("Kategori Popular",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,color=Color(0xFF0D47A1));Spacer(Modifier.height(10.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){CategoryCard("🍔","Makanan",Color(0xFFE3F2FD),Modifier.weight(1f));CategoryCard("👕","Fashion",Color(0xFFEAF3FF),Modifier.weight(1f));CategoryCard("💄","Beauty",Color(0xFFF5F9FF),Modifier.weight(1f))}}};item{Card(modifier=Modifier.padding(horizontal=16.dp).fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Color(0xFF1565C0)),shape=RoundedCornerShape(22.dp)){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("PROMO HARI ANI 🎉",color=Color.White,fontWeight=FontWeight.ExtraBold);Text("Support local seller & discover barang menarik dekat kitani.",color=Color.White)};Text("🇧🇳",style=MaterialTheme.typography.headlineMedium)}}};item{Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column{Text("Barang Berdekatan",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,color=Color(0xFF0D47A1));Text("Pilihan seller sekitar Brunei",style=MaterialTheme.typography.bodySmall,color=Color.Gray)};if(isSeller)FilledTonalButton(onClick=seller){Text("Dashboard")}}};if(ps.isEmpty())item{Text("Barang inda jumpa",modifier=Modifier.padding(16.dp))};if(ps.isNotEmpty())items(ps){p->ColorfulProductCard(p,add,view,Modifier.padding(horizontal=16.dp))}}}

@Composable fun CategoryCard(icon:String,label:String,bg:Color,m:Modifier=Modifier){Card(modifier=m,colors=CardDefaults.cardColors(containerColor=bg),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(vertical=14.dp,horizontal=8.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(icon,style=MaterialTheme.typography.headlineSmall);Text(label,fontWeight=FontWeight.SemiBold,style=MaterialTheme.typography.labelMedium,color=Color(0xFF0D47A1))}}}
@Composable fun ColorfulProductCard(p:Product,add:(Product)->Unit,view:(Product)->Unit,m:Modifier=Modifier){Card(modifier=m.fillMaxWidth(),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Color.White),border=CardDefaults.outlinedCardBorder()){Column(Modifier.padding(14.dp)){Row(verticalAlignment=Alignment.CenterVertically){ProductImage(p.image,Modifier.size(76.dp).clip(RoundedCornerShape(18.dp)));Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(p.name,fontWeight=FontWeight.Bold);Text(p.shop,color=Color(0xFF1565C0),fontWeight=FontWeight.SemiBold);Text("📍 ${p.area} • ${p.category}",style=MaterialTheme.typography.bodySmall,color=Color.Gray);Text("B$%.2f".format(p.price),fontWeight=FontWeight.ExtraBold,color=Color(0xFF0D47A1))}};Spacer(Modifier.height(10.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={view(p)},modifier=Modifier.weight(1f)){Text("Lihat Produk")};Button(onClick={add(p)},modifier=Modifier.weight(1f)){Text("Tambah")}}}}}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ProductDetailSheet(product:Product,onClose:()->Unit,onAdd:()->Unit){ModalBottomSheet(onDismissRequest=onClose){LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){item{ProductImage(product.image,Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(24.dp)))};item{Text(product.name,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.ExtraBold);Text("B$%.2f".format(product.price),fontWeight=FontWeight.ExtraBold,color=Color(0xFF1565C0),style=MaterialTheme.typography.titleLarge)};item{Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFEAF3FF))){Column(Modifier.padding(16.dp)){Text("Seller Profile",fontWeight=FontWeight.ExtraBold,color=Color(0xFF0D47A1));Text("🏪 ${product.shop}");Text("📞 ${product.sellerPhone}");Text("🏠 ${product.sellerAddress}");Text("📍 ${product.area}")}}};item{Button(onClick=onAdd,modifier=Modifier.fillMaxWidth()){Text("Tambah ke Troli • B$%.2f".format(product.price))}};item{Spacer(Modifier.height(24.dp))}}}}

@Composable fun SearchPage(ps:List<Product>,q:String,setQ:(String)->Unit,add:(Product)->Unit,view:(Product)->Unit,m:Modifier){Column(m.fillMaxSize().background(Color.White).padding(16.dp)){Text("Cari barang",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,color=Color(0xFF0D47A1));Spacer(Modifier.height(12.dp));SearchBox(q,setQ);Spacer(Modifier.height(14.dp));if(ps.isEmpty())Text("Tiada hasil carian")else ProductList(ps,add,view)}}
@Composable fun SearchBox(q:String,setQ:(String)->Unit){Surface(shape=RoundedCornerShape(20.dp),color=Color.White,shadowElevation=3.dp,modifier=Modifier.fillMaxWidth()){OutlinedTextField(value=q,onValueChange=setQ,modifier=Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Search barang, kedai atau kategori...")},leadingIcon={Text("🔎")},trailingIcon=if(q.isNotBlank()){{TextButton(onClick={setQ("")}){Text("✕")}}}else null,shape=RoundedCornerShape(20.dp))}}
@Composable fun ProductList(ps:List<Product>,add:(Product)->Unit,view:(Product)->Unit){LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(ps){p->ColorfulProductCard(p,add,view)}}}

@Composable
fun CartPage(c:List<CartLine>,minus:(Product)->Unit,plus:(Product)->Unit,remove:(Product)->Unit,checkout:(String,String,Double,String,Double)->Unit,m:Modifier){
    var delivery by remember{mutableStateOf("Delivery")}
    var district by remember{mutableStateOf("Brunei-Muara")}
    var payment by remember{mutableStateOf("Cash")}
    val subtotal=c.sumOf{it.product.price*it.qty}
    val fee=if(c.isEmpty()||delivery=="Pickup")0.0 else deliveryFeeFor(district)
    Column(m.padding(16.dp)){
        Text("Troli",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,color=Color(0xFF0D47A1))
        Spacer(Modifier.height(12.dp))
        if(c.isEmpty()) Text("Troli masih kosong") else {
            LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
                items(c){x->
                    Card(Modifier.fillMaxWidth()){
                        Column(Modifier.padding(12.dp)){
                            Row(verticalAlignment=Alignment.CenterVertically){
                                ProductImage(x.product.image,Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)){
                                    Text(x.product.name,fontWeight=FontWeight.Bold)
                                    money(x.product.price)
                                }
                                TextButton(onClick={remove(x.product)}){Text("Remove")}
                            }
                            Row(verticalAlignment=Alignment.CenterVertically){
                                Text("Qty",color=Color.Gray)
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick={minus(x.product)}){Text("−")}
                                Text(x.qty.toString(),fontWeight=FontWeight.Bold)
                                TextButton(onClick={plus(x.product)}){Text("+")}
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=delivery=="Delivery",onClick={delivery="Delivery"},label={Text("Delivery")});FilterChip(selected=delivery=="Pickup",onClick={delivery="Pickup"},label={Text("Pickup")})}
            if(delivery=="Delivery"){
                Text("Pilih district delivery",fontWeight=FontWeight.Bold)
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){FilterChip(selected=district=="Brunei-Muara",onClick={district="Brunei-Muara"},label={Text("B-Muara B$3")});FilterChip(selected=district=="Tutong",onClick={district="Tutong"},label={Text("Tutong B$5")})}
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){FilterChip(selected=district=="Kuala Belait",onClick={district="Kuala Belait"},label={Text("KB B$6")});FilterChip(selected=district=="Temburong",onClick={district="Temburong"},label={Text("Temburong B$8")})}
            }
            Text("Payment",fontWeight=FontWeight.Bold)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=payment=="Cash",onClick={payment="Cash"},label={Text("💵 Cash")});FilterChip(selected=payment=="Transfer",onClick={payment="Transfer"},label={Text("🏦 Transfer")})}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Subtotal");money(subtotal)}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(if(delivery=="Pickup")"Pickup fee" else "Delivery $district");money(fee)}
            HorizontalDivider(Modifier.padding(vertical=8.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Jumlah",fontWeight=FontWeight.Bold);money(subtotal+fee)}
            Button(onClick={checkout(delivery,if(delivery=="Pickup")"Pickup" else district,fee,payment,subtotal+fee)},modifier=Modifier.fillMaxWidth()){Text("Place Order")}
        }
    }
}

@Composable
fun ProfilePage(orders:List<Order>,user:UserAccount,seller:()->Unit,logout:()->Unit,markDelivered:(String)->Unit,m:Modifier){
    val activeOrders=orders.filter{it.deliveredAt==null}
    val historyOrders=orders.filter{it.deliveredAt!=null}
    LazyColumn(modifier=m.fillMaxSize().background(Color.White),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Text("Akaun",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,color=Color(0xFF0D47A1));Text("👤 ${user.username}");Text("Role: ${user.role}",color=Color.Gray)}
        if(user.role=="Seller")item{Button(onClick=seller,modifier=Modifier.fillMaxWidth()){Text("🏪 Seller Dashboard")}}
        item{OutlinedButton(onClick=logout,modifier=Modifier.fillMaxWidth()){Text("Logout")}}
        item{Text("Pesanan Aktif",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
        if(activeOrders.isEmpty())item{Text("Tiada pesanan aktif")}
        if(activeOrders.isNotEmpty())items(activeOrders.reversed()){order->LiveOrderCard(order,onDelivered={markDelivered(order.id)})}
        item{HorizontalDivider();Text("History Delivery",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,color=Color(0xFF0D47A1))}
        if(historyOrders.isEmpty())item{Text("Belum ada delivery selesai")}
        if(historyOrders.isNotEmpty())items(historyOrders.sortedByDescending{it.deliveredAt}){order->DeliveredOrderCard(order)}
    }
}

@Composable
fun LiveOrderCard(order:Order,onDelivered:()->Unit){
    var stage by remember(order.id){mutableIntStateOf(0)}
    var showTracking by remember(order.id){mutableStateOf(false)}
    val stages=if(order.delivery=="Pickup") listOf("Order diterima","Sedang disediakan","Sedia untuk pickup","Selesai") else listOf("Order diterima","Seller menyiapkan barang","Runner collect semua seller","Dalam perjalanan","Sampai")
    LaunchedEffect(order.id){
        while(stage<stages.lastIndex){delay(7000);stage++}
        if(order.deliveredAt==null) onDelivered()
    }
    val sellerGroups=order.lines.groupBy{it.product.shop}
    Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Color.White),modifier=Modifier.fillMaxWidth()){
        Column(Modifier.padding(16.dp)){
            Text("Order #${order.id}",fontWeight=FontWeight.Bold)
            Text(stages[stage],fontWeight=FontWeight.ExtraBold,color=Color(0xFF1565C0))
            LinearProgressIndicator(progress={(stage+1).toFloat()/stages.size.toFloat()},modifier=Modifier.fillMaxWidth())
            Text("B$%.2f".format(order.total),fontWeight=FontWeight.Bold)
            Text("Payment: ${order.paymentMethod}")
            Text(if(order.delivery=="Pickup")"Pickup" else "Delivery: ${order.district} • B$%.2f".format(order.deliveryFee),color=Color.Gray)
            Text("Pickup seller (${sellerGroups.size})",fontWeight=FontWeight.Bold)
            sellerGroups.forEach{(shop,lines)->Text("• $shop — ${lines.sumOf{it.qty}} item")}
            if(order.delivery!="Pickup"){
                Text("Runner KadaiKu",fontWeight=FontWeight.Bold)
                Text("🛵 ${order.runnerName}")
                Text("📞 ${order.runnerPhone}")
            }
            OutlinedButton(onClick={showTracking=!showTracking},modifier=Modifier.fillMaxWidth()){
                Text(if(showTracking)"Tutup Tracking" else "📍 Live Tracking")
            }
            if(showTracking){
                Text(if(order.delivery=="Pickup")"Pickup order — tiada runner delivery." else "${order.runnerName} akan collect dari ${sellerGroups.size} seller sebelum menghantar ke ${order.district}.",color=Color.Gray)
            }
        }
    }
}

@Composable
fun DeliveredOrderCard(order:Order){
    Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFFF5F9FF)),modifier=Modifier.fillMaxWidth()){
        Column(Modifier.padding(14.dp)){
            Text("Order #${order.id}",fontWeight=FontWeight.Bold)
            Text(if(order.delivery=="Pickup")"✅ Pickup selesai" else "✅ Delivery sampai",fontWeight=FontWeight.ExtraBold,color=Color(0xFF1565C0))
            Text("Payment: ${order.paymentMethod}")
            Text("Jumlah buyer: B$%.2f".format(order.total))
            Text(if(order.delivery=="Pickup")"Pickup" else "${order.district} • Runner ${order.runnerName}",color=Color.Gray)
        }
    }
}

fun startOfWeek(now:Long):Long{val c=Calendar.getInstance().apply{timeInMillis=now;set(Calendar.DAY_OF_WEEK,firstDayOfWeek);set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)};return c.timeInMillis}
fun startOfMonth(now:Long):Long{val c=Calendar.getInstance().apply{timeInMillis=now;set(Calendar.DAY_OF_MONTH,1);set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)};return c.timeInMillis}
fun startOfYear(now:Long):Long{val c=Calendar.getInstance().apply{timeInMillis=now;set(Calendar.DAY_OF_YEAR,1);set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)};return c.timeInMillis}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SellerSheet(onClose:()->Unit,onAdd:()->Unit,products:List<Product>,orders:List<Order>,profile:SellerProfile,sellerId:String,onDelete:(String)->Unit){
    val now=System.currentTimeMillis()
    val sellerOrders=orders.filter{o->o.lines.any{it.product.sellerId==sellerId}}
    val weekOrders=sellerOrders.filter{it.createdAt>=startOfWeek(now)}
    val monthOrders=sellerOrders.filter{it.createdAt>=startOfMonth(now)}
    val yearOrders=sellerOrders.filter{it.createdAt>=startOfYear(now)}
    val mine=products.filter{it.sellerId==sellerId}
    val completedSellerOrders=sellerOrders.filter{it.deliveredAt!=null}
    ModalBottomSheet(onDismissRequest=onClose){
        LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            item{Text("${profile.shop} Dashboard",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,color=Color(0xFF0D47A1));Text("Dashboard khas seller account ani",color=Color.Gray)}
            item{Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFEAF3FF))){Column(Modifier.padding(14.dp)){Text("Seller Contact",fontWeight=FontWeight.Bold);Text("📞 ${profile.phone}");Text("🏠 ${profile.address}")}}}
            item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                StatCard("Minggu",weekOrders.size,weekOrders.sumOf{sellerOrderValue(it,sellerId)},Modifier.weight(1f))
                StatCard("Bulan",monthOrders.size,monthOrders.sumOf{sellerOrderValue(it,sellerId)},Modifier.weight(1f))
                StatCard("Tahun",yearOrders.size,yearOrders.sumOf{sellerOrderValue(it,sellerId)},Modifier.weight(1f))
            }}
            item{Text("Jumlah jualan hanya barang kedai ani — delivery fee dan barang seller lain inda dikira.",style=MaterialTheme.typography.bodySmall,color=Color.Gray)}
            item{Button(onClick=onAdd,modifier=Modifier.fillMaxWidth()){Text("+ Tambah Produk")}}
            item{Text("Produk saya",fontWeight=FontWeight.Bold)}
            if(mine.isEmpty())item{Text("Belum ada produk")}
            if(mine.isNotEmpty())items(mine){p->Card(Modifier.fillMaxWidth()){Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){ProductImage(p.image,Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(p.name,fontWeight=FontWeight.Bold);money(p.price)};TextButton(onClick={onDelete(p.id)}){Text("Padam")}}}}
            item{Text("Order untuk kedai saya",fontWeight=FontWeight.Bold)}
            if(sellerOrders.isEmpty())item{Text("Belum ada order")}
            if(sellerOrders.isNotEmpty())items(sellerOrders.reversed()){o->
                val own=o.lines.filter{it.product.sellerId==sellerId}
                val ownValue=sellerOrderValue(o,sellerId)
                Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){
                    Text("#${o.id}",fontWeight=FontWeight.Bold)
                    Text("${own.sumOf{it.qty}} item dari kedai ani")
                    Text("Jualan kedai ani: B$%.2f".format(ownValue),fontWeight=FontWeight.Bold,color=Color(0xFF1565C0))
                    Text("Payment: ${o.paymentMethod}")
                    Text(if(o.deliveredAt!=null)"✅ Selesai / History" else if(o.delivery=="Pickup")"Pickup" else "Delivery: ${o.district}")
                }}
            }
            item{Text("History Seller",fontWeight=FontWeight.Bold,color=Color(0xFF0D47A1))}
            if(completedSellerOrders.isEmpty())item{Text("Belum ada order selesai")}
            if(completedSellerOrders.isNotEmpty())items(completedSellerOrders.sortedByDescending{it.deliveredAt}){o->
                Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Color(0xFFF5F9FF))){Column(Modifier.padding(12.dp)){
                    Text("#${o.id} • Selesai",fontWeight=FontWeight.Bold)
                    Text("Jualan kedai ani: B$%.2f".format(sellerOrderValue(o,sellerId)))
                }}
            }
            item{Spacer(Modifier.height(30.dp))}
        }
    }
}

@Composable fun StatCard(label:String,count:Int,sales:Double,m:Modifier=Modifier){Card(modifier=m,shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.padding(12.dp)){Text(label,fontWeight=FontWeight.Bold);Text("$count order",style=MaterialTheme.typography.bodySmall,color=Color.Gray);Text("B$%.2f".format(sales),fontWeight=FontWeight.ExtraBold,color=Color(0xFF1565C0))}}}
@Composable fun AddProductDialog(sellerProfile:SellerProfile,onDismiss:()->Unit,onSave:(String,Double,String,String,String)->Unit){var n by remember{mutableStateOf("")};var pr by remember{mutableStateOf("")};var cat by remember{mutableStateOf("Makanan")};var area by remember{mutableStateOf("")};var imageUri by remember{mutableStateOf("")};val launcher=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri:Uri?->imageUri=uri?.toString().orEmpty()};AlertDialog(onDismissRequest=onDismiss,title={Text("Tambah Produk")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Gambar produk wajib",fontWeight=FontWeight.Bold);if(imageUri.isNotBlank())ProductImage(imageUri,Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp)))else Text("🖼️ Belum pilih gambar");OutlinedButton(onClick={launcher.launch("image/*")},modifier=Modifier.fillMaxWidth()){Text(if(imageUri.isBlank())"Pilih gambar dari Gallery" else "Tukar gambar")};OutlinedTextField(n,{n=it},label={Text("Nama produk")},modifier=Modifier.fillMaxWidth());OutlinedTextField(pr,{pr=it},label={Text("Harga B$")},modifier=Modifier.fillMaxWidth());OutlinedTextField(cat,{cat=it},label={Text("Kategori")},modifier=Modifier.fillMaxWidth());OutlinedTextField(area,{area=it},label={Text("Kawasan")},modifier=Modifier.fillMaxWidth());Text("Seller: ${sellerProfile.shop}");Text("📞 ${sellerProfile.phone}");Text("🏠 ${sellerProfile.address}")}},confirmButton={Button(enabled=n.isNotBlank()&&pr.toDoubleOrNull()!=null&&area.isNotBlank()&&imageUri.isNotBlank(),onClick={onSave(n,pr.toDouble(),cat,area,imageUri)}){Text("Simpan")}},dismissButton={TextButton(onClick=onDismiss){Text("Batal")}})}
