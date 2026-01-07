import React, { createContext, useState, useContext } from 'react';

// create context
const CartContext = createContext();

// create a custom hook to use the context easily
export const useCart = () => useContext(CartContext);

// create the provider that will handle the cart logic
export const CartProvider = ({ children }) => {
  const [cartItems, setCartItems] = useState([]);

  const addToCart = (tool) => {
    // Prevent adding the same item twice
    if (!cartItems.some(item => item.id === tool.id)) {
      setCartItems([...cartItems, tool]);
    }
  };

  const removeFromCart = (toolId) => {
    setCartItems(cartItems.filter(item => item.id !== toolId));
  };

  const clearCart = () => {
    setCartItems([]);
  };

  const value = {
    cartItems,
    addToCart,
    removeFromCart,
    clearCart,
  };

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
};