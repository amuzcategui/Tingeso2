import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

function CompleteProfile() {
  const [phone, setPhone] = useState('');
  const [birthDate, setBirthDate] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (event) => {
    event.preventDefault();
    try {
      await customerService.completeProfile({ phone, birthDate }); 
      navigate('/home');
    } catch (err) {
      setError('No se pudo completar el perfil.');
    }
  };

  return (
    <div>
      <h1>Completa tu Perfil</h1>
      <p>Necesitamos algunos datos adicionales para continuar.</p>
      <form onSubmit={handleSubmit}>
        <div>
          <label>Teléfono:</label>
          <input type="tel" value={phone} onChange={e => setPhone(e.target.value)} required />
        </div>
        <div>
          <label>Fecha de Nacimiento:</label>
          <input type="date" value={birthDate} onChange={e => setBirthDate(e.target.value)} required />
        </div>
        <button type="submit">Guardar y Continuar</button>
        {error && <p style={{color: 'red'}}>{error}</p>}
      </form>
    </div>
  );
}

export default CompleteProfile;