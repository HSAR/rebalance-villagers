package net.minecraft.server.v1_7_R3;

import java.util.*;

public class EntityVillager extends EntityAgeable implements IMerchant, NPC
{
    private int profession;
    private boolean br;
    private boolean bs;
    Village village;
    private EntityHuman tradingPlayer;
    private MerchantRecipeList bu;
    private int bv;
    private boolean bw;
    private int riches;
    private String by;
    private boolean bz;
    private float bA;
    
    public EntityVillager(World paramWorld)
    {
        this(paramWorld, 0);
    }
    
    public EntityVillager(World paramWorld, int paramInt)
    {
        super(paramWorld);
        setProfession(paramInt);
        a(0.6F, 1.8F);
        
        getNavigation().b(true);
        getNavigation().a(true);
        
        this.goalSelector.a(0, new PathfinderGoalFloat(this));
        this.goalSelector.a(1, new PathfinderGoalAvoidPlayer(this, EntityZombie.class,
                8.0F, 0.6D, 0.6D));
        this.goalSelector.a(1, new PathfinderGoalTradeWithPlayer(this));
        this.goalSelector.a(1, new PathfinderGoalLookAtTradingPlayer(this));
        this.goalSelector.a(2, new PathfinderGoalMoveIndoors(this));
        this.goalSelector.a(3, new PathfinderGoalRestrictOpenDoor(this));
        this.goalSelector.a(4, new PathfinderGoalOpenDoor(this, true));
        this.goalSelector.a(5, new PathfinderGoalMoveTowardsRestriction(this, 0.6D));
        this.goalSelector.a(6, new PathfinderGoalMakeLove(this));
        this.goalSelector.a(7, new PathfinderGoalTakeFlower(this));
        this.goalSelector.a(8, new PathfinderGoalPlay(this, 0.32D));
        this.goalSelector.a(9, new PathfinderGoalInteract(this, EntityHuman.class, 3.0F,
                1.0F));
        this.goalSelector.a(9, new PathfinderGoalInteract(this, EntityVillager.class,
                5.0F, 0.02F));
        this.goalSelector.a(9, new PathfinderGoalRandomStroll(this, 0.6D));
        this.goalSelector.a(10, new PathfinderGoalLookAtPlayer(this,
                EntityInsentient.class, 8.0F));
    }
    
    protected void aC()
    {
        super.aC();
        
        getAttributeInstance(GenericAttributes.d).setValue(0.5D);
    }
    
    public boolean bj()
    {
        return true;
    }
    
    protected void bo()
    {
        Object localObject;
        if (--this.profession <= 0)
        {
            this.world.villages.a(MathHelper.floor(this.locX),
                    MathHelper.floor(this.locY), MathHelper.floor(this.locZ));
            this.profession = (70 + this.random.nextInt(50));
            
            this.village = this.world.villages.getClosestVillage(
                    MathHelper.floor(this.locX), MathHelper.floor(this.locY),
                    MathHelper.floor(this.locZ), 32);
            if (this.village == null)
            {
                bX();
            }
            else
            {
                localObject = this.village.getCenter();
                a(((ChunkCoordinates) localObject).x, ((ChunkCoordinates) localObject).y,
                        ((ChunkCoordinates) localObject).z,
                        (int) (this.village.getSize() * 0.6F));
                if (this.bz)
                {
                    this.bz = false;
                    this.village.b(5);
                }
            }
        }
        if ((!cc()) && (this.bv > 0))
        {
            this.bv -= 1;
            if (this.bv <= 0)
            {
                if (this.bw)
                {
                    if (this.bu.size() > 1)
                    {
                        for (localObject = this.bu.iterator(); ((Iterator) localObject)
                                .hasNext();)
                        {
                            MerchantRecipe localMerchantRecipe = (MerchantRecipe) ((Iterator) localObject)
                                    .next();
                            if (localMerchantRecipe.g())
                            {
                                localMerchantRecipe.a(this.random.nextInt(6)
                                        + this.random.nextInt(6) + 2);
                            }
                        }
                    }
                    t(1);
                    this.bw = false;
                    if ((this.village != null) && (this.by != null))
                    {
                        this.world.broadcastEntityEffect(this, (byte) 14);
                        this.village.a(this.by, 1);
                    }
                }
                addEffect(new MobEffect(MobEffectList.REGENERATION.id, 200, 0));
            }
        }
        super.bo();
    }
    
    public boolean a(EntityHuman paramEntityHuman)
    {
        ItemStack localItemStack = paramEntityHuman.inventory.getItemInHand();
        int i = (localItemStack != null)
                && (localItemStack.getItem() == Items.MONSTER_EGG) ? 1 : 0;
        if ((i == 0) && (isAlive()) && (!cc()) && (!isBaby()))
        {
            if (!this.world.isStatic)
            {
                a_(paramEntityHuman);
                paramEntityHuman.openTrade(this, getCustomName());
            }
            return true;
        }
        return super.a(paramEntityHuman);
    }
    
    protected void c()
    {
        super.c();
        this.datawatcher.a(16, Integer.valueOf(0));
    }
    
    public void b(NBTTagCompound paramNBTTagCompound)
    {
        super.b(paramNBTTagCompound);
        paramNBTTagCompound.setInt("Profession", getProfession());
        paramNBTTagCompound.setInt("Riches", this.riches);
        if (this.bu != null)
        {
            paramNBTTagCompound.set("Offers", this.bu.a());
        }
    }
    
    public void a(NBTTagCompound paramNBTTagCompound)
    {
        super.a(paramNBTTagCompound);
        setProfession(paramNBTTagCompound.getInt("Profession"));
        this.riches = paramNBTTagCompound.getInt("Riches");
        if (paramNBTTagCompound.hasKeyOfType("Offers", 10))
        {
            NBTTagCompound localNBTTagCompound = paramNBTTagCompound
                    .getCompound("Offers");
            this.bu = new MerchantRecipeList(localNBTTagCompound);
        }
    }
    
    protected boolean isTypeNotPersistent()
    {
        return false;
    }
    
    protected String t()
    {
        if (cc())
        {
            return "mob.villager.haggle";
        }
        return "mob.villager.idle";
    }
    
    protected String aS()
    {
        return "mob.villager.hit";
    }
    
    protected String aT()
    {
        return "mob.villager.death";
    }
    
    public void setProfession(int paramInt)
    {
        this.datawatcher.watch(16, Integer.valueOf(paramInt));
    }
    
    public int getProfession()
    {
        return this.datawatcher.getInt(16);
    }
    
    public boolean ca()
    {
        return this.br;
    }
    
    public void i(boolean paramBoolean)
    {
        this.br = paramBoolean;
    }
    
    public void j(boolean paramBoolean)
    {
        this.bs = paramBoolean;
    }
    
    public boolean cb()
    {
        return this.bs;
    }
    
    public void b(EntityLiving paramEntityLiving)
    {
        super.b(paramEntityLiving);
        if ((this.village != null) && (paramEntityLiving != null))
        {
            this.village.a(paramEntityLiving);
            if ((paramEntityLiving instanceof EntityHuman))
            {
                int i = -1;
                if (isBaby())
                {
                    i = -3;
                }
                this.village.a(paramEntityLiving.getName(), i);
                if (isAlive())
                {
                    this.world.broadcastEntityEffect(this, (byte) 13);
                }
            }
        }
    }
    
    public void die(DamageSource paramDamageSource)
    {
        if (this.village != null)
        {
            Entity localEntity = paramDamageSource.getEntity();
            if (localEntity != null)
            {
                if ((localEntity instanceof EntityHuman))
                {
                    this.village.a(localEntity.getName(), -2);
                }
                else if ((localEntity instanceof IMonster))
                {
                    this.village.h();
                }
            }
            else if (localEntity == null)
            {
                EntityHuman localEntityHuman = this.world.findNearbyPlayer(this, 16.0D);
                if (localEntityHuman != null)
                {
                    this.village.h();
                }
            }
        }
        super.die(paramDamageSource);
    }
    
    public void a_(EntityHuman paramEntityHuman)
    {
        this.tradingPlayer = paramEntityHuman;
    }
    
    public EntityHuman b()
    {
        return this.tradingPlayer;
    }
    
    public boolean cc()
    {
        return this.tradingPlayer != null;
    }
    
    public void a(MerchantRecipe paramMerchantRecipe)
    {
        paramMerchantRecipe.f();
        this.a_ = (-q());
        makeSound("mob.villager.yes", be(), bf());
        if (paramMerchantRecipe.a((MerchantRecipe) this.bu.get(this.bu.size() - 1)))
        {
            this.bv = 40;
            this.bw = true;
            if (this.tradingPlayer != null)
            {
                this.by = this.tradingPlayer.getName();
            }
            else
            {
                this.by = null;
            }
        }
        if (paramMerchantRecipe.getBuyItem1().getItem() == Items.EMERALD)
        {
            this.riches += paramMerchantRecipe.getBuyItem1().count;
        }
    }
    
    public void a_(ItemStack paramItemStack)
    {
        if ((!this.world.isStatic) && (this.a_ > -q() + 20))
        {
            this.a_ = (-q());
            if (paramItemStack != null)
            {
                makeSound("mob.villager.yes", be(), bf());
            }
            else
            {
                makeSound("mob.villager.no", be(), bf());
            }
        }
    }
    
    public MerchantRecipeList getOffers(EntityHuman paramEntityHuman)
    {
        if (this.bu == null)
        {
            t(1);
        }
        return this.bu;
    }
    
    private float p(float paramFloat)
    {
        float f = paramFloat + this.bA;
        if (f > 0.9F)
        {
            return 0.9F - (f - 0.9F);
        }
        return f;
    }
    
    private void t(int paramInt)
    {
        if (this.bu != null)
        {
            this.bA = (MathHelper.c(this.bu.size()) * 0.2F);
        }
        else
        {
            this.bA = 0.0F;
        }
        MerchantRecipeList localMerchantRecipeList = new MerchantRecipeList();
        Object localObject1;
        int m;
        switch (getProfession())
        {
            case 0:
                a(localMerchantRecipeList, Items.WHEAT, this.random, p(0.9F));
                a(localMerchantRecipeList, Item.getItemOf(Blocks.WOOL), this.random,
                        p(0.5F));
                a(localMerchantRecipeList, Items.RAW_CHICKEN, this.random, p(0.5F));
                a(localMerchantRecipeList, Items.COOKED_FISH, this.random, p(0.4F));
                b(localMerchantRecipeList, Items.BREAD, this.random, p(0.9F));
                b(localMerchantRecipeList, Items.MELON, this.random, p(0.3F));
                b(localMerchantRecipeList, Items.APPLE, this.random, p(0.3F));
                b(localMerchantRecipeList, Items.COOKIE, this.random, p(0.3F));
                b(localMerchantRecipeList, Items.SHEARS, this.random, p(0.3F));
                b(localMerchantRecipeList, Items.FLINT_AND_STEEL, this.random, p(0.3F));
                b(localMerchantRecipeList, Items.COOKED_CHICKEN, this.random, p(0.3F));
                b(localMerchantRecipeList, Items.ARROW, this.random, p(0.5F));
                if (this.random.nextFloat() < p(0.5F))
                {
                    localMerchantRecipeList.add(new MerchantRecipe(new ItemStack(
                            Blocks.GRAVEL, 10), new ItemStack(Items.EMERALD),
                            new ItemStack(Items.FLINT, 4 + this.random.nextInt(2), 0)));
                }
                break;
            case 4:
                a(localMerchantRecipeList, Items.COAL, this.random, p(0.7F));
                a(localMerchantRecipeList, Items.PORK, this.random, p(0.5F));
                a(localMerchantRecipeList, Items.RAW_BEEF, this.random, p(0.5F));
                b(localMerchantRecipeList, Items.SADDLE, this.random, p(0.1F));
                b(localMerchantRecipeList, Items.LEATHER_CHESTPLATE, this.random, p(0.3F));
                b(localMerchantRecipeList, Items.LEATHER_BOOTS, this.random, p(0.3F));
                b(localMerchantRecipeList, Items.LEATHER_HELMET, this.random, p(0.3F));
                b(localMerchantRecipeList, Items.LEATHER_LEGGINGS, this.random, p(0.3F));
                b(localMerchantRecipeList, Items.GRILLED_PORK, this.random, p(0.3F));
                b(localMerchantRecipeList, Items.COOKED_BEEF, this.random, p(0.3F));
                break;
            case 3:
                a(localMerchantRecipeList, Items.COAL, this.random, p(0.7F));
                a(localMerchantRecipeList, Items.IRON_INGOT, this.random, p(0.5F));
                a(localMerchantRecipeList, Items.GOLD_INGOT, this.random, p(0.5F));
                a(localMerchantRecipeList, Items.DIAMOND, this.random, p(0.5F));
                
                b(localMerchantRecipeList, Items.IRON_SWORD, this.random, p(0.5F));
                b(localMerchantRecipeList, Items.DIAMOND_SWORD, this.random, p(0.5F));
                b(localMerchantRecipeList, Items.IRON_AXE, this.random, p(0.3F));
                b(localMerchantRecipeList, Items.DIAMOND_AXE, this.random, p(0.3F));
                b(localMerchantRecipeList, Items.IRON_PICKAXE, this.random, p(0.5F));
                b(localMerchantRecipeList, Items.DIAMOND_PICKAXE, this.random, p(0.5F));
                b(localMerchantRecipeList, Items.IRON_SPADE, this.random, p(0.2F));
                b(localMerchantRecipeList, Items.DIAMOND_SPADE, this.random, p(0.2F));
                b(localMerchantRecipeList, Items.IRON_HOE, this.random, p(0.2F));
                b(localMerchantRecipeList, Items.DIAMOND_HOE, this.random, p(0.2F));
                b(localMerchantRecipeList, Items.IRON_BOOTS, this.random, p(0.2F));
                b(localMerchantRecipeList, Items.DIAMOND_BOOTS, this.random, p(0.2F));
                b(localMerchantRecipeList, Items.IRON_HELMET, this.random, p(0.2F));
                b(localMerchantRecipeList, Items.DIAMOND_HELMET, this.random, p(0.2F));
                b(localMerchantRecipeList, Items.IRON_CHESTPLATE, this.random, p(0.2F));
                b(localMerchantRecipeList, Items.DIAMOND_CHESTPLATE, this.random, p(0.2F));
                b(localMerchantRecipeList, Items.IRON_LEGGINGS, this.random, p(0.2F));
                b(localMerchantRecipeList, Items.DIAMOND_LEGGINGS, this.random, p(0.2F));
                b(localMerchantRecipeList, Items.CHAINMAIL_BOOTS, this.random, p(0.1F));
                b(localMerchantRecipeList, Items.CHAINMAIL_HELMET, this.random, p(0.1F));
                b(localMerchantRecipeList, Items.CHAINMAIL_CHESTPLATE, this.random,
                        p(0.1F));
                b(localMerchantRecipeList, Items.CHAINMAIL_LEGGINGS, this.random, p(0.1F));
                break;
            case 1:
                a(localMerchantRecipeList, Items.PAPER, this.random, p(0.8F));
                a(localMerchantRecipeList, Items.BOOK, this.random, p(0.8F));
                a(localMerchantRecipeList, Items.WRITTEN_BOOK, this.random, p(0.3F));
                b(localMerchantRecipeList, Item.getItemOf(Blocks.BOOKSHELF), this.random,
                        p(0.8F));
                b(localMerchantRecipeList, Item.getItemOf(Blocks.GLASS), this.random,
                        p(0.2F));
                b(localMerchantRecipeList, Items.COMPASS, this.random, p(0.2F));
                b(localMerchantRecipeList, Items.WATCH, this.random, p(0.2F));
                if (this.random.nextFloat() < p(0.07F))
                {
                    localObject1 = Enchantment.c[this.random
                            .nextInt(Enchantment.c.length)];
                    int j = MathHelper.nextInt(this.random,
                            ((Enchantment) localObject1).getStartLevel(),
                            ((Enchantment) localObject1).getMaxLevel());
                    ItemStack localItemStack = Items.ENCHANTED_BOOK
                            .a(new EnchantmentInstance((Enchantment) localObject1, j));
                    m = 2 + this.random.nextInt(5 + j * 10) + 3 * j;
                    
                    localMerchantRecipeList
                            .add(new MerchantRecipe(new ItemStack(Items.BOOK),
                                    new ItemStack(Items.EMERALD, m), localItemStack));
                }
                break;
            case 2:
                b(localMerchantRecipeList, Items.EYE_OF_ENDER, this.random, p(0.3F));
                b(localMerchantRecipeList, Items.EXP_BOTTLE, this.random, p(0.2F));
                b(localMerchantRecipeList, Items.REDSTONE, this.random, p(0.4F));
                b(localMerchantRecipeList, Item.getItemOf(Blocks.GLOWSTONE), this.random,
                        p(0.3F));
                
                localObject1 = new Item[] { Items.IRON_SWORD, Items.DIAMOND_SWORD,
                        Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.IRON_AXE,
                        Items.DIAMOND_AXE, Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE };
                for (Item localItem : localObject1)
                {
                    if (this.random.nextFloat() < p(0.05F))
                    {
                        localMerchantRecipeList.add(new MerchantRecipe(new ItemStack(
                                localItem, 1, 0), new ItemStack(Items.EMERALD,
                                2 + this.random.nextInt(3), 0), EnchantmentManager.a(
                                this.random, new ItemStack(localItem, 1, 0),
                                5 + this.random.nextInt(15))));
                    }
                }
        }
        if (localMerchantRecipeList.isEmpty())
        {
            a(localMerchantRecipeList, Items.GOLD_INGOT, this.random, 1.0F);
        }
        Collections.shuffle(localMerchantRecipeList);
        if (this.bu == null)
        {
            this.bu = new MerchantRecipeList();
        }
        for (int i = 0; (i < paramInt) && (i < localMerchantRecipeList.size()); i++)
        {
            this.bu.a((MerchantRecipe) localMerchantRecipeList.get(i));
        }
    }
    
    private static final Map bB = new HashMap();
    private static final Map bC = new HashMap();
    
    static
    {
        bB.put(Items.COAL, new Tuple(Integer.valueOf(16), Integer.valueOf(24)));
        bB.put(Items.IRON_INGOT, new Tuple(Integer.valueOf(8), Integer.valueOf(10)));
        bB.put(Items.GOLD_INGOT, new Tuple(Integer.valueOf(8), Integer.valueOf(10)));
        bB.put(Items.DIAMOND, new Tuple(Integer.valueOf(4), Integer.valueOf(6)));
        bB.put(Items.PAPER, new Tuple(Integer.valueOf(24), Integer.valueOf(36)));
        bB.put(Items.BOOK, new Tuple(Integer.valueOf(11), Integer.valueOf(13)));
        bB.put(Items.WRITTEN_BOOK, new Tuple(Integer.valueOf(1), Integer.valueOf(1)));
        bB.put(Items.ENDER_PEARL, new Tuple(Integer.valueOf(3), Integer.valueOf(4)));
        bB.put(Items.EYE_OF_ENDER, new Tuple(Integer.valueOf(2), Integer.valueOf(3)));
        bB.put(Items.PORK, new Tuple(Integer.valueOf(14), Integer.valueOf(18)));
        bB.put(Items.RAW_BEEF, new Tuple(Integer.valueOf(14), Integer.valueOf(18)));
        bB.put(Items.RAW_CHICKEN, new Tuple(Integer.valueOf(14), Integer.valueOf(18)));
        bB.put(Items.COOKED_FISH, new Tuple(Integer.valueOf(9), Integer.valueOf(13)));
        bB.put(Items.SEEDS, new Tuple(Integer.valueOf(34), Integer.valueOf(48)));
        bB.put(Items.MELON_SEEDS, new Tuple(Integer.valueOf(30), Integer.valueOf(38)));
        bB.put(Items.PUMPKIN_SEEDS, new Tuple(Integer.valueOf(30), Integer.valueOf(38)));
        bB.put(Items.WHEAT, new Tuple(Integer.valueOf(18), Integer.valueOf(22)));
        bB.put(Item.getItemOf(Blocks.WOOL),
                new Tuple(Integer.valueOf(14), Integer.valueOf(22)));
        bB.put(Items.ROTTEN_FLESH, new Tuple(Integer.valueOf(36), Integer.valueOf(64)));
        
        bC.put(Items.FLINT_AND_STEEL, new Tuple(Integer.valueOf(3), Integer.valueOf(4)));
        bC.put(Items.SHEARS, new Tuple(Integer.valueOf(3), Integer.valueOf(4)));
        bC.put(Items.IRON_SWORD, new Tuple(Integer.valueOf(7), Integer.valueOf(11)));
        bC.put(Items.DIAMOND_SWORD, new Tuple(Integer.valueOf(12), Integer.valueOf(14)));
        bC.put(Items.IRON_AXE, new Tuple(Integer.valueOf(6), Integer.valueOf(8)));
        bC.put(Items.DIAMOND_AXE, new Tuple(Integer.valueOf(9), Integer.valueOf(12)));
        bC.put(Items.IRON_PICKAXE, new Tuple(Integer.valueOf(7), Integer.valueOf(9)));
        bC.put(Items.DIAMOND_PICKAXE, new Tuple(Integer.valueOf(10), Integer.valueOf(12)));
        bC.put(Items.IRON_SPADE, new Tuple(Integer.valueOf(4), Integer.valueOf(6)));
        bC.put(Items.DIAMOND_SPADE, new Tuple(Integer.valueOf(7), Integer.valueOf(8)));
        bC.put(Items.IRON_HOE, new Tuple(Integer.valueOf(4), Integer.valueOf(6)));
        bC.put(Items.DIAMOND_HOE, new Tuple(Integer.valueOf(7), Integer.valueOf(8)));
        bC.put(Items.IRON_BOOTS, new Tuple(Integer.valueOf(4), Integer.valueOf(6)));
        bC.put(Items.DIAMOND_BOOTS, new Tuple(Integer.valueOf(7), Integer.valueOf(8)));
        bC.put(Items.IRON_HELMET, new Tuple(Integer.valueOf(4), Integer.valueOf(6)));
        bC.put(Items.DIAMOND_HELMET, new Tuple(Integer.valueOf(7), Integer.valueOf(8)));
        bC.put(Items.IRON_CHESTPLATE, new Tuple(Integer.valueOf(10), Integer.valueOf(14)));
        bC.put(Items.DIAMOND_CHESTPLATE,
                new Tuple(Integer.valueOf(16), Integer.valueOf(19)));
        bC.put(Items.IRON_LEGGINGS, new Tuple(Integer.valueOf(8), Integer.valueOf(10)));
        bC.put(Items.DIAMOND_LEGGINGS,
                new Tuple(Integer.valueOf(11), Integer.valueOf(14)));
        bC.put(Items.CHAINMAIL_BOOTS, new Tuple(Integer.valueOf(5), Integer.valueOf(7)));
        bC.put(Items.CHAINMAIL_HELMET, new Tuple(Integer.valueOf(5), Integer.valueOf(7)));
        bC.put(Items.CHAINMAIL_CHESTPLATE,
                new Tuple(Integer.valueOf(11), Integer.valueOf(15)));
        bC.put(Items.CHAINMAIL_LEGGINGS,
                new Tuple(Integer.valueOf(9), Integer.valueOf(11)));
        bC.put(Items.BREAD, new Tuple(Integer.valueOf(-4), Integer.valueOf(-2)));
        bC.put(Items.MELON, new Tuple(Integer.valueOf(-8), Integer.valueOf(-4)));
        bC.put(Items.APPLE, new Tuple(Integer.valueOf(-8), Integer.valueOf(-4)));
        bC.put(Items.COOKIE, new Tuple(Integer.valueOf(-10), Integer.valueOf(-7)));
        bC.put(Item.getItemOf(Blocks.GLASS),
                new Tuple(Integer.valueOf(-5), Integer.valueOf(-3)));
        bC.put(Item.getItemOf(Blocks.BOOKSHELF),
                new Tuple(Integer.valueOf(3), Integer.valueOf(4)));
        bC.put(Items.LEATHER_CHESTPLATE,
                new Tuple(Integer.valueOf(4), Integer.valueOf(5)));
        bC.put(Items.LEATHER_BOOTS, new Tuple(Integer.valueOf(2), Integer.valueOf(4)));
        bC.put(Items.LEATHER_HELMET, new Tuple(Integer.valueOf(2), Integer.valueOf(4)));
        bC.put(Items.LEATHER_LEGGINGS, new Tuple(Integer.valueOf(2), Integer.valueOf(4)));
        bC.put(Items.SADDLE, new Tuple(Integer.valueOf(6), Integer.valueOf(8)));
        bC.put(Items.EXP_BOTTLE, new Tuple(Integer.valueOf(-4), Integer.valueOf(-1)));
        bC.put(Items.REDSTONE, new Tuple(Integer.valueOf(-4), Integer.valueOf(-1)));
        bC.put(Items.COMPASS, new Tuple(Integer.valueOf(10), Integer.valueOf(12)));
        bC.put(Items.WATCH, new Tuple(Integer.valueOf(10), Integer.valueOf(12)));
        bC.put(Item.getItemOf(Blocks.GLOWSTONE),
                new Tuple(Integer.valueOf(-3), Integer.valueOf(-1)));
        bC.put(Items.GRILLED_PORK, new Tuple(Integer.valueOf(-7), Integer.valueOf(-5)));
        bC.put(Items.COOKED_BEEF, new Tuple(Integer.valueOf(-7), Integer.valueOf(-5)));
        bC.put(Items.COOKED_CHICKEN, new Tuple(Integer.valueOf(-8), Integer.valueOf(-6)));
        bC.put(Items.EYE_OF_ENDER, new Tuple(Integer.valueOf(7), Integer.valueOf(11)));
        bC.put(Items.ARROW, new Tuple(Integer.valueOf(-12), Integer.valueOf(-8)));
    }
    
    private static void a(MerchantRecipeList paramMerchantRecipeList, Item paramItem,
            Random paramRandom, float paramFloat)
    {
        if (paramRandom.nextFloat() < paramFloat)
        {
            paramMerchantRecipeList.add(new MerchantRecipe(a(paramItem, paramRandom),
                    Items.EMERALD));
        }
    }
    
    private static ItemStack a(Item paramItem, Random paramRandom)
    {
        return new ItemStack(paramItem, b(paramItem, paramRandom), 0);
    }
    
    private static int b(Item paramItem, Random paramRandom)
    {
        Tuple localTuple = (Tuple) bB.get(paramItem);
        if (localTuple == null)
        {
            return 1;
        }
        if (((Integer) localTuple.a()).intValue() >= ((Integer) localTuple.b())
                .intValue())
        {
            return ((Integer) localTuple.a()).intValue();
        }
        return ((Integer) localTuple.a()).intValue()
                + paramRandom.nextInt(((Integer) localTuple.b()).intValue()
                        - ((Integer) localTuple.a()).intValue());
    }
    
    private static void b(MerchantRecipeList paramMerchantRecipeList, Item paramItem,
            Random paramRandom, float paramFloat)
    {
        if (paramRandom.nextFloat() < paramFloat)
        {
            int i = c(paramItem, paramRandom);
            ItemStack localItemStack1;
            ItemStack localItemStack2;
            if (i < 0)
            {
                localItemStack1 = new ItemStack(Items.EMERALD, 1, 0);
                localItemStack2 = new ItemStack(paramItem, -i, 0);
            }
            else
            {
                localItemStack1 = new ItemStack(Items.EMERALD, i, 0);
                localItemStack2 = new ItemStack(paramItem, 1, 0);
            }
            paramMerchantRecipeList.add(new MerchantRecipe(localItemStack1,
                    localItemStack2));
        }
    }
    
    private static int c(Item paramItem, Random paramRandom)
    {
        Tuple localTuple = (Tuple) bC.get(paramItem);
        if (localTuple == null)
        {
            return 1;
        }
        if (((Integer) localTuple.a()).intValue() >= ((Integer) localTuple.b())
                .intValue())
        {
            return ((Integer) localTuple.a()).intValue();
        }
        return ((Integer) localTuple.a()).intValue()
                + paramRandom.nextInt(((Integer) localTuple.b()).intValue()
                        - ((Integer) localTuple.a()).intValue());
    }
    
    public GroupDataEntity a(GroupDataEntity paramGroupDataEntity)
    {
        paramGroupDataEntity = super.a(paramGroupDataEntity);
        
        setProfession(this.world.random.nextInt(5));
        
        return paramGroupDataEntity;
    }
    
    public void cd()
    {
        this.bz = true;
    }
    
    public EntityVillager b(EntityAgeable paramEntityAgeable)
    {
        EntityVillager localEntityVillager = new EntityVillager(this.world);
        localEntityVillager.a(null);
        return localEntityVillager;
    }
    
    public boolean bM()
    {
        return false;
    }
}
