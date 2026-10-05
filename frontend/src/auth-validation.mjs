export function validateCredentials({username,password}) {
 const errors={};
 if(!/^[a-zA-Z0-9_.-]{3,40}$/.test(username||'')) errors.username=(username||'').includes('@')?'Usuario: escribe un nombre, no un email. Por ejemplo: sami2game.':'Usuario: usa 3-40 letras sin tildes, números, punto, guion o guion bajo; sin espacios.';
 if(!password||password.length<12) errors.password='Contraseña: escribe al menos 12 caracteres.';
 else if(password.length>72||new TextEncoder().encode(password).length>72) errors.password='Contraseña: máximo 72 caracteres y 72 bytes. Las tildes y los emojis pueden ocupar varios bytes.';
 return errors;
}
