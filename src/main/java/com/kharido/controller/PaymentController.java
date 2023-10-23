package com.kharido.controller;

import com.kharido.entity.Order;
import com.kharido.exception.OrderException;
import com.kharido.repository.OrderRepository;
import com.kharido.response.ApiResponse;
import com.kharido.response.PaymentLinkResponse;
import com.kharido.service.OrderService;
import com.kharido.service.UserService;
import com.razorpay.Payment;
import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PaymentController {

    @Value("${razorpay.api.key}")
    String apiKey;

    @Value("${razorpay.api.secret}")
    String apiSecret;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderRepository orderRepository;

    @PostMapping("/payments/{orderId}")
    public ResponseEntity<PaymentLinkResponse> createPaymentLink(@PathVariable Long orderId,
                                                                 @RequestHeader("Authorization") String jwt) throws RazorpayException {
        Order order = orderService.findOrderById(orderId);

        try {
            RazorpayClient razorpay = new RazorpayClient(apiKey,apiSecret);

            JSONObject paymentLinkRequest = new JSONObject();

            paymentLinkRequest.put("amount",order.getTotalDiscountedPrice()*100);
            paymentLinkRequest.put("currency","INR");

            JSONObject customer = new JSONObject();
            customer.put("name",order.getUser().getFirstName());
            customer.put("email",order.getUser().getEmail());
            paymentLinkRequest.put("customer",customer);

            JSONObject notify = new JSONObject();
            notify.put("sms",true);
            notify.put("email",true);
            paymentLinkRequest.put("notify",notify);

            paymentLinkRequest.put("callback_url","https://shailesh-ecommerce.vercel.app/payment/"+orderId);
            paymentLinkRequest.put("callback_method","get");

            PaymentLink payment = razorpay.paymentLink.create(paymentLinkRequest);

            String paymentLinkId = payment.get("id");
            String paymentLinkUrl = payment.get("short_url");

            PaymentLinkResponse res = new PaymentLinkResponse();
            res.setPayment_link_id(paymentLinkId);
            res.setPayment_link_url(paymentLinkUrl);

            return new ResponseEntity<PaymentLinkResponse>(res, HttpStatus.CREATED);
        } catch (RazorpayException e) {
            throw new RazorpayException(e.getMessage());
        }
    }

    @GetMapping("/payments")
    public ResponseEntity<ApiResponse> redirect(@RequestParam(name = "payment_id") String paymentId,
                                                @RequestParam(name = "order_id") Long orderId) throws OrderException, RazorpayException {
        Order order = orderService.findOrderById(orderId);
        RazorpayClient razorpay = new RazorpayClient(apiKey,apiSecret);

        try{
            Payment payment = razorpay.payments.fetch(paymentId);
            if(payment.get("status").equals("captured")){
             order.getPaymentDetails().setPaymentId(paymentId);
             order.getPaymentDetails().setStatus("COMPLETED");
             order.setOrderStatus("PLACED");
             orderRepository.save(order);
            }

            ApiResponse apiResponse = new ApiResponse("your order get placed",true);
            return new ResponseEntity<>(apiResponse,HttpStatus.ACCEPTED);
        }
        catch (Exception e){
            throw new RazorpayException(e.getMessage());
        }
    }
}
