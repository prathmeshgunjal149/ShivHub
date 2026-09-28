export const formatAddress = address => [address.addressLine1,address.addressLine2,address.landmark,address.city,address.district,address.state,address.pincode].filter(Boolean).join(", ");
